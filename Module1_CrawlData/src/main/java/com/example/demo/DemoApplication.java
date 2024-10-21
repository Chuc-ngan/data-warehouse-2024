package com.example.demo;

import com.example.demo.dao.ConfigDAO;
import com.example.demo.model.*;
import com.example.demo.service.ConfigService;
import com.example.demo.service.LogService;
import com.example.demo.service.crawler.CrawlService;
import com.example.demo.service.emailService.EmailServiceImpl;
import com.example.demo.service.emailService.IEmailService;
import com.example.demo.utils.CsvReader;
import com.example.demo.utils.CsvWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

	@Autowired
	private CrawlService crawlService;

	private final EmailServiceImpl emailService;
	private final ConfigService configService;
	private final LogService logService;
	@Autowired
	private ExecutorService executorService;
	@Autowired
	public DemoApplication(EmailServiceImpl emailService, ConfigService configService, LogService logService) {
		this.emailService = emailService;
		this.configService = configService;
		this.logService = logService;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		CsvReader csvReader = new CsvReader();
		List<Config> configs = configService.getAllConfigs();

		Optional<Config> runningConfigOptional = configService.getConfigRunning();

		// Nếu có config đang chạy, gửi email thông báo và dừng lại
		if (runningConfigOptional.isPresent()) {
			Config runningConfig = runningConfigOptional.get();
			String message = "Có một config đang chạy: " + runningConfig.getId() + ". Không bắt đầu crawl.";
			logService.logCrawlEvent(runningConfig.getId(), LogLevel.WARNING, Status.SUCCESS_EXTRACT, message, "", 0, 0);
			emailService.sendFailureEmail(runningConfig.getNotificationEmails(), message);
			return; // Dừng lại nếu có config đang chạy
		}

		List<Future<?>> futures = new ArrayList<>();

		configs.stream()
				.filter(Config::isActive) // Chỉ chọn config đang hoạt động
				.forEach(config -> {
					Future<?> future = executorService.submit(() -> {
						runCrawlForConfig(config, csvReader);
					});
					futures.add(future);
				});

		// Chờ cho tất cả các công việc hoàn thành
		for (Future<?> future : futures) {
			try {
				future.get(); // Chờ cho công việc hoàn thành
			} catch (Exception e) {
				System.err.println("Có lỗi xảy ra khi chạy config: " + e.getMessage());
			}
		}
	}

	private void runCrawlForConfig(Config readyConfig, CsvReader csvReader) {
		if (!readyConfig.getStatus().equals(Status.READY_EXTRACT)) {
			System.err.println("Config " + readyConfig.getId() + " không ở trạng thái sẵn sàng. Bỏ qua crawl.");
			return; // Nếu không ở trạng thái sẵn sàng, dừng lại
		}

		// Cập nhật trạng thái thành "đang chạy" và ghi log
		readyConfig.setStatus(Status.PROCESSING);
		configService.updateConfig(readyConfig);
		logService.logCrawlEvent(readyConfig.getId(), LogLevel.INFO, Status.PROCESSING,
				"Bắt đầu crawl với config.", "", 0, 0);
    
		try {
			System.out.println("Bắt đầu crawl với config: " + readyConfig.getId());
			String currentDirectory = readyConfig.getDestinationPath();
			String csvFilePath = currentDirectory + FileSystems.getDefault().getSeparator()
					+ readyConfig.getFilePath() + FileSystems.getDefault().getSeparator()
					+ "products_id.csv";

			List<String> productIds = csvReader.readProductIdsFromCsv(csvFilePath);
			List<String> limitedProductIds = productIds.size() > readyConfig.getDataSize() ?
					productIds.subList(5, readyConfig.getDataSize()) : productIds;

			boolean crawlSuccess = false;
			int retryAttempts = 0;
			List<Product> products = List.of();
			String outputCsvFilePath = "";

			// Bắt đầu tính toán thời gian crawl
			long startTime = System.currentTimeMillis();

			while (!crawlSuccess && retryAttempts < readyConfig.getRetryCount()) {
				try {
					products = crawlService.crawlProducts(limitedProductIds);
					readyConfig.setLastCrawlTime(LocalDateTime.now());
					readyConfig.setStatus(Status.SUCCESS_EXTRACT);
					configService.updateConfig(readyConfig);

					// Ghi kết quả vào tệp CSV
					CsvWriter csvWriter = new CsvWriter();
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
					String timestamp = dateFormat.format(new Date());
					outputCsvFilePath = currentDirectory + FileSystems.getDefault().getSeparator()
							+ readyConfig.getFilePath() + FileSystems.getDefault().getSeparator()
							+ readyConfig.getFileName() + "_" + timestamp + ".csv";
					csvWriter.writeProductsToCsv(products, outputCsvFilePath);

					// Gửi email thông báo thành công
					emailService.sendSuccessEmail(readyConfig.getNotificationEmails(), outputCsvFilePath, products.size());
					System.out.println("Crawl thành công!");

					crawlSuccess = true;
				} catch (Exception e) {
					retryAttempts++;
					String message = "Có lỗi xảy ra khi crawl config " + readyConfig.getId() + ": " + e.getMessage() +
							" (Thử lại lần " + retryAttempts + ")";
					handleError(readyConfig, message, e);
				}
			}

			if (crawlSuccess) {
				long endTime = System.currentTimeMillis();
				long duration = endTime - startTime;
				logService.logCrawlEvent(readyConfig.getId(), LogLevel.INFO, Status.SUCCESS_EXTRACT,
						"Crawl hoàn thành trong " + duration + " ms", "", products.size(), 0);
			} else {
				String message = "Crawl không thành công sau " + retryAttempts + " lần thử.";
				handleError(readyConfig, message, new Exception("Crawl không thành công"));
			}
		} catch (Exception e) {
			handleError(readyConfig, "Lỗi không xác định khi chạy crawl với config " + readyConfig.getId(), e);
		}
	}

	// Phương thức xử lý lỗi để giảm thiểu trùng lặp mã
	private void handleError(Config readyConfig, String message, Exception e) {
		readyConfig.setStatus(Status.FAILURE_EXTRACT);
		configService.updateConfig(readyConfig);
		String stackTrace = Arrays.toString(e.getStackTrace());
		emailService.sendFailureEmail(readyConfig.getNotificationEmails(), message);
		logService.logCrawlEvent(readyConfig.getId(), LogLevel.ERROR, Status.FAILURE_EXTRACT, message, stackTrace, 1, 0);
		System.err.println(message);
	}


//		CsvReader csvReader = new CsvReader();
//		List<Config> configs = configService.getAllConfigs();
//
//		Optional<Config> runningConfigOptional = configService.getConfigRunning();
//
//		// Nếu có config đang chạy, gửi email thông báo và dừng lại
//		if (runningConfigOptional.isPresent()) {
//			Config runningConfig = runningConfigOptional.get();
//			logService.logCrawlEvent(runningConfig.getId(), LogLevel.WARNING, Status.SUCCESS_EXTRACT,
//					"Có một config đang chạy. Không bắt đầu crawl.", "", 0, 0);
//			emailService.sendFailureEmail(runningConfig.getNotificationEmails(),
//					"Có một config đang chạy. Không bắt đầu crawl.");
//			return; // Dừng lại nếu có config đang chạy
//		}
//
//		ExecutorService executorService = Executors.newFixedThreadPool(5);
//		List<CompletableFuture<Void>> futures = new ArrayList<>();
//
//		configs.stream()
//				.filter(Config::isActive)
//				.forEach(config -> {
//					futures.add(CompletableFuture.runAsync(() -> {
//						configService.getReadyConfig()
//								.ifPresentOrElse(readyConfig -> {
//											System.out.println("Bắt đầu crawl với config: " + readyConfig);
//											String currentDirectory = readyConfig.getDestinationPath();
//											String csvFilePath = currentDirectory + FileSystems.getDefault().getSeparator()
//													+ readyConfig.getFilePath() + FileSystems.getDefault().getSeparator()
//													+ "products_id.csv";
//
//											List<String> productIds = csvReader.readProductIdsFromCsv(csvFilePath);
//											List<String> limitedProductIds = productIds.size() > readyConfig.getDataSize() ?
//													productIds.subList(0, readyConfig.getDataSize()) : productIds;
//
//											boolean crawlSuccess = false;
//											int retryAttempts = 0;
//											List<Product> products = List.of();
//											String outputCsvFilePath = "";
//											// Bắt đầu tính toán thời gian crawl
//											long startTime = System.currentTimeMillis();
//											while (!crawlSuccess && retryAttempts < readyConfig.getRetryCount()) {
//												try {
//													products = crawlService.crawlProducts(limitedProductIds);
//													readyConfig.setLastCrawlTime(LocalDateTime.now());
//													readyConfig.setStatus(Status.SUCCESS_EXTRACT);
//													configService.updateConfig(readyConfig);
//
//													CsvWriter csvWriter = new CsvWriter();
//													SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
//													String timestamp = dateFormat.format(new Date());
//													outputCsvFilePath = currentDirectory + FileSystems.getDefault().getSeparator()
//															+ readyConfig.getFilePath() + FileSystems.getDefault().getSeparator()
//															+ readyConfig.getFileName() + timestamp + ".csv";
//													csvWriter.writeProductsToCsv(products, outputCsvFilePath);
//
//													emailService.sendSuccessEmail(readyConfig.getNotificationEmails(), outputCsvFilePath, products.size());
//													System.out.println("Crawl thành công!");
//
//													crawlSuccess = true; // Đánh dấu crawl thành công
//												} catch (InterruptedException e) {
//													retryAttempts++;
//													System.err.println("Crawl bị gián đoạn: " + e.getMessage() + ". Thử lại " + retryAttempts + "/" + readyConfig.getRetryCount());
//												} catch (JsonProcessingException e) {
//													handleError(readyConfig, "Có lỗi trong việc xử lý JSON: " + e.getMessage(), e);
//													break; // Không thử lại nếu có lỗi JSON
//												} catch (Exception e) {
//													handleError(readyConfig, "Có lỗi xảy ra: " + e.getMessage(), e);
//													break; // Không thử lại nếu có lỗi khác
//												}
//
//												// Đợi trước khi thử lại
//												try {
//													Thread.sleep(readyConfig.getCrawlFrequency() * 1000);
//												} catch (InterruptedException e) {
//													Thread.currentThread().interrupt(); // Đặt lại trạng thái gián đoạn
//													System.err.println("Gián đoạn trong khi chờ: " + e.getMessage());
//												}
//											}
//											// Kết thúc tính toán thời gian crawl
//											long endTime = System.currentTimeMillis();
//											long totalTime = endTime - startTime;
//											if (crawlSuccess) {
//												logService.logCrawlEvent(readyConfig.getId(), LogLevel.INFO, Status.SUCCESS_EXTRACT,
//														"Crawl thành công! Sản phẩm đã được ghi vào: " + outputCsvFilePath + ". Tổng thời gian crawl: " + (totalTime / 1000.0) + " giây.", "", products.size(), totalTime);
//												System.out.println("Tổng thời gian crawl: " + (totalTime / 1000.0) + " giây."); // Hiển thị thời gian crawl
//											} else {
//												readyConfig.setStatus(Status.FAILURE_EXTRACT);
//												configService.updateConfig(readyConfig);
//												System.err.println("Crawl thất bại sau " + readyConfig.getRetryCount() + " lần thử.");
//											}
//										},
//										() -> System.out.println("Không có config nào đang sẵn sàng. Không bắt đầu crawl.")
//								);
//					}, executorService));
//				});
//
//		// Chờ tất cả các task hoàn thành
//		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//		executorService.shutdown(); // Đóng executor service sau khi hoàn thành
//	}






}
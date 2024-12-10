package com.example.demo;

import com.example.demo.model.*;
import com.example.demo.service.ConfigService;
import com.example.demo.service.LogService;
import com.example.demo.service.crawler.CrawlService;
import com.example.demo.service.emailService.EmailServiceImpl;
import com.example.demo.utils.CsvReader;
import com.example.demo.utils.CsvWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@SpringBootApplication
public class DemoApplication {

	@Autowired
	private static CrawlService crawlService;
	private static  EmailServiceImpl emailService;
	private static  ConfigService configService;
	private static  LogService logService;

	@Autowired
	private static ExecutorService executorService;


	private static ConfigurableApplicationContext context;

	@Autowired
	public DemoApplication(CrawlService crawlService, EmailServiceImpl emailService, ConfigService configService, LogService logService,  ExecutorService executorService) {
		this.crawlService = crawlService;
		this.emailService = emailService;
		this.configService = configService;
		this.logService = logService;
		this.executorService = executorService;
	}

	public static void main(String[] args) {
		// Bắt đầu Spring Boot application
		context = SpringApplication.run(DemoApplication.class, args);

		// Sau khi Spring Boot đã khởi động, bạn có thể gọi các phương thức xử lý riêng của mình
		DemoApplication application = new DemoApplication(crawlService, emailService, configService, logService, executorService);
		try {
			application.runApp();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Dừng ứng dụng sau khi hoàn tất
			SpringApplication.exit(context);
		}
	}

	public void runApp() throws Exception {
		// 3. Lấy ra danh sách các Config đang hoạt động (is_active = true)
		List<Config> configs = configService.getActiveConfigs();

		checkAndAddLogIfNotExists(configs);

		CsvReader csvReader = new CsvReader();

		// 5. Kiểm tra xem có config nào đang ở trạng thái "PROCESSING" hay không?
		if (checkAndNotifyRunningConfigs()) return;

		List<Future<?>> futures = new ArrayList<>();

		for (Config config : configs) {
			Future<?> future = executorService.submit(() -> {
				runCrawlForConfig(config, csvReader);
			});
			futures.add(future);
		}

		// Chờ cho tất cả các công việc hoàn thành
		for (Future<?> future : futures) {
			try {
				future.get(); // Chờ cho công việc hoàn thành
			} catch (Exception e) {
				System.err.println("Có lỗi xảy ra khi chạy config: " + e.getMessage());
			}
		}
		executorService.shutdown();
		try {
			// Chờ tối đa 60 giây * 3 để tất cả các công việc hoàn thành
			if (!executorService.awaitTermination(60*3, TimeUnit.SECONDS)) {
				executorService.shutdownNow();// Nếu không hoàn thành, dừng ngay lập tức
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
	}

	/**
	 * Kiểm tra xem hôm nay có log nào không?
	 * Nếu có bỏ qua.
	 * Nếu không thêm log mới dựa trên các config đang hoạt động.
	 * @param configs
	 **/
	private void checkAndAddLogIfNotExists(List<Config> configs) {
		for (Config config : configs) {
			// 4. Kiểm tra xem từng config hôm nay có tồn tại log nào không?
			boolean todayLogExists = logService.isTodayLogExistsForConfig(config.getId());

			if (!todayLogExists) {
				// 4.1 Tạo log mới với status mặc định là "READY_EXTRACT".
				Log log = new Log();
				log.setIdConfig(config.getId());
				log.setStatus(Status.READY_EXTRACT);
				log.setCreateTime(LocalDateTime.now());
				log.setUpdateTime(LocalDateTime.now());
				log.setLogLevel(LogLevel.INFO);
				log.setCreatedBy("Admin");
				log.setLocation("Crawl data");

				// Lưu log vào cơ sở dữ liệu
				logService.saveLog(log);
			} else {
				System.out.println("Log cho config với ID " + config.getId() + " đã tồn tại hôm nay, bỏ qua việc thêm log.");
			}
		}
    }

	/**
	 * Phương thức kiểm tra và thông báo nếu có cấu hình đang chạy
	 */
	private boolean checkAndNotifyRunningConfigs() {
		// 5.1 Đếm số lượng dòng trong bảng logs mà có trạng thái là 'PROCESSING' và thời gian tạo (create_time) là ngày hiện tại
		boolean isRunning = logService.isConfigRunning();

		if (isRunning) {
			// 5.2. Lấy danh sách các Config có status 'PROCESSING' trong ngày hôm nay.
			List<Config> runningConfigs = logService.getProcessingStatusesToday();
			System.out.println("Running configs count: " + runningConfigs.size());
			if (!runningConfigs.isEmpty()) {
				//5.3  Tạo một message với danh sách các ID của các config đang chạy.
				String ids = runningConfigs.stream()
						.map(Config::getId)
						.map(String::valueOf)
						.collect(Collectors.joining(", "));

				//5.4 Tạo thông báo cho các config đang chạy
				String message = "Có các config đang chạy: " + ids + ". Không bắt đầu crawl.";
				System.out.println(message);
				// 5.5  Gửi email thông báo tới các địa chỉ được chỉ định trong config cho từng config
				runningConfigs.forEach(config -> {
					emailService.sendFailureEmail(config.getNotificationEmails(), message);
				});

				return true; // Có config đang chạy, dừng lại
			}
		}
		System.out.println("Không có config nào đang chạy");
		return false;
	}

	//	Phương thức lấy danh sách ID sản phẩm giới hạn từ file CSV
	private List<String> getLimitedProductIds(Config readyConfig, CsvReader csvReader) {
		//9. Từ các đường dãn cấu hình trong config lấy ra đường dẫn đến file CSV chứa ID sản phẩm và dataSize từ bảng config
		String currentDirectory = readyConfig.getFilePath();
		String csvFilePath = currentDirectory + FileSystems.getDefault().getSeparator()
				+ readyConfig.getDestinationPath() + FileSystems.getDefault().getSeparator()
				+ "products_id.csv";
		List<String> productIds = csvReader.readProductIdsFromCsv(csvFilePath);
		List<String> limitedProductIds;
		// 10. Lấy danh sách ID sản phẩm giới hạn từ file CSV
		int dataSize = readyConfig.getDataSize();

		// 11. Kiểm tra danh sách sản phẩm đọc được có lớn hơn dataSize của config?
		if (productIds.size() > dataSize) {
			//12.Trộn danh sách sản phẩm ngẫu nhiên và Lấy số lượng sản phẩm dựa trên dataSize
			Collections.shuffle(productIds); // Trộn danh sách sản phẩm ngẫu nhiên
			limitedProductIds = productIds.subList(0, dataSize); // Lấy số lượng sản phẩm dựa trên dataSize
		} else {
			//11.1 Lấy toàn bộ danh sách sản phẩm
			limitedProductIds = productIds; // Nếu danh sách có ít hơn hoặc bằng dataSize, lấy toàn bộ danh sách
		}

		return limitedProductIds;
	}

	private void runCrawlForConfig(Config readyConfig, CsvReader csvReader) {
		// Kiểm tra nếu cấu hình là null
		if (readyConfig == null) {
			System.err.println("Cấu hình không hợp lệ. Bỏ qua crawl.");
			return; // Nếu cấu hình không hợp lệ, dừng lại
		}

		//6. Lấy log của config có stutus =  READY_EXTRACT trong ngày hiện tại từ id_config
		Log log = logService.getConfigLogByStatusToday(readyConfig.getId());

		//7.  Kiểm tra xem log có null không?
		if (log != null) {
			System.out.println("Cấu hình " + readyConfig.getId() + " đã sẵn sàng để crawl.");
			//8. Update trạng thái = PROCESSING và thời gian update = thời gian hiện tại của log lại
			log.setStatus(Status.PROCESSING);
			log.setUpdateTime(LocalDateTime.now());
			logService.updateLog(log);

			List<String> limitedProductIds = getLimitedProductIds(readyConfig, csvReader);
			boolean crawlSuccess = executeCrawl(readyConfig, limitedProductIds, log);
			if (!crawlSuccess) {
				// Xử lý lỗi khi crawl thất bại
				handleCrawlFailure(readyConfig);
			}

		} else {
			String message = "Config " + readyConfig.getId() + " không ở trạng thái READY_EXTRACT. Bỏ qua crawl.";
			// 7.1 Gửi email thông báo tới các địa chỉ được chỉ định trong config cho từng config nào có log có status = "READY_EXTRACT"
			emailService.sendFailureEmail(readyConfig.getNotificationEmails(), message);
		}

	}

	private void handleCrawlFailure(Config readyConfig) {

	}

	private boolean executeCrawl(Config readyConfig, List<String> limitedProductIds, Log log) {
		// 13. Khởi tạo crawlSuccess = false để đánh dấu trạng thái thành công của quá trình crawl,
		// retryAttempts = 0 để đếm số lần thử lại nếu crawl thất bại
		boolean crawlSuccess = false;
		// Đếm số lần thử lại nếu crawl thất bại
		int retryAttempts = 0;

		// Bắt đầu tính toán thời gian thực hiện crawl
		long startTime = System.currentTimeMillis();

		// 14.  Lặp lại cho đến khi crawl thành công hoặc vượt quá số lần thử tối đa
		//while !crawlSuccess AND retryAttempts < maxRetries

		//15. Kiểm tra !crawlSuccess AND retryAttempts < maxRetries có thỏa không?
		while (!crawlSuccess && retryAttempts < readyConfig.getRetryCount()) {
			try {
				// 16. Gọi server crawl để lấy danh sách sản phẩm bằng cách truyền vào url của trang web trong config
				// và limitedProductIds để gọi crawlService.crawlProducts(limitedProductIds, readyConfig.getSourcePath()); để lấy sản phẩm

				//17. Kiểm tra nếu có lỗi trong quá trình crawl (Try-Catch)
				System.out.println("Đang tiến hành crawl config " + readyConfig.getId() + "...");
				List<Product> products = crawlService.crawlProducts(limitedProductIds, readyConfig.getSourcePath());

				// 18. Tạo folder backup CSV bằng cách lấy tên file và đường dẫn từ config và định dạng thời gian yyyyMMdd_HHmm
				CsvWriter csvWriter = new CsvWriter();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
				String timestamp = dateFormat.format(new Date());
				String currentDirectory = readyConfig.getFilePath();
				String tempDirPath = currentDirectory + FileSystems.getDefault().getSeparator()
						+ readyConfig.getBackupPath();

				Path tempDir = Paths.get(tempDirPath);

				// 19. Kiểm tra nếu thư mục tạm chưa tồn tại chưa?
				if (!Files.exists(tempDir)) {
					//19.1. Tạo thư mục backup
					Files.createDirectories(tempDir);
					System.out.println("Thư mục tạm đã được tạo: " + tempDirPath);
				}

				// 20. Tạo file tạm với định dạnh temp_[tên file]_yyyyMMdd_HHmm.csv
				String tempCsvFilePath = tempDirPath + FileSystems.getDefault().getSeparator()
						+ "temp_" + readyConfig.getFileName() + "_" + timestamp + ".csv";

				// 21. Ghi danh sách sản phẩm vào tệp CSV tạm
				csvWriter.writeProductsToCsv(products, tempCsvFilePath);

				// 22. Kiểm tra file tạm có tồn tại và hợp lệ không?
				Path tempFilePath = Paths.get(tempCsvFilePath);
				if (Files.exists(tempFilePath)) {
					System.out.println("Tệp tạm tạo thành công: " + tempCsvFilePath);

					// Đường dẫn file chính thức
					String outputCsvFilePath = currentDirectory + FileSystems.getDefault().getSeparator()
							+ readyConfig.getDestinationPath() + FileSystems.getDefault().getSeparator()
							+ readyConfig.getFileName() + "_" + timestamp + ".csv";

					// 23. Di chuyển tệp tạm thành tệp chính với định dạng [tên file]_yyyyMMdd_HHmm.csv
					Files.move(tempFilePath, Paths.get(outputCsvFilePath), StandardCopyOption.REPLACE_EXISTING);

					System.out.println("File tạm đã chuyển thành file chính: " + outputCsvFilePath);

					// 24. Update trạng thái = SUCCESS_EXTRACT và thời gian update = thời gian hiện tại của log lại
					long endTime = System.currentTimeMillis();
					long duration = endTime - startTime;
					String successMessage = "Crawl thành công cho cấu hình " + readyConfig.getId() + " trong " + duration + " ms.";
					log.setErrorMessage(successMessage);
					log.setDestinationPath(outputCsvFilePath);
					log.setCount(products.size());
					log.setUpdateTime(LocalDateTime.now());
					log.setStatus(Status.SUCCESS_EXTRACT);
					logService.updateLog(log);

					// 25. Gửi email thông báo tới các địa chỉ được chỉ định trong config thông báo thành công và gửi file đã crawl được vào email.
					emailService.sendSuccessEmail(readyConfig.getNotificationEmails(), outputCsvFilePath, products.size(), LocalDateTime.now());
					System.out.println("Crawl thành công config " + readyConfig.getId() + "!");
					// 26. Cập nhật crawlSuccess = true (hoàn thành).
					crawlSuccess = true;
				} else {
					// 22. 1 Ghi log lỗi: không tìm thấy tệp tạm
					retryAttempts++;
					System.err.println("Không thể tìm thấy file tạm: " + tempCsvFilePath);
				}
			// 17.1 Xác định lỗi gì JSON, InterruptedException,..
			} catch (InterruptedException e) {
				// Xử lý lỗi khi bị gián đoạn
				String errorMessage = "Crawl bị gián đoạn: " + e.getMessage();
				System.err.println(errorMessage);
				handleError(readyConfig, log, errorMessage, e);
				break; // Dừng nếu bị gián đoạn

			} catch (JsonProcessingException e) {
				// Xử lý lỗi JSON
				String errorMessage = "Lỗi trong quá trình xử lý JSON: " + e.getMessage();
				System.err.println(errorMessage);
				handleError(readyConfig, log, errorMessage, e);

			} catch (Exception e) {
				// Xử lý các lỗi khác
				String errorMessage = "Có lỗi xảy ra khi crawl: " + e.getMessage();
				System.err.println(errorMessage);
				handleError(readyConfig, log, errorMessage, e);
			}

			// 22.2 Tăng retryAttempts
			retryAttempts++;

			// 22.3 Chờ một khoảng thời gian = CrawlFrequency load lên từ table config
			try {
				Thread.sleep(readyConfig.getCrawlFrequency() * 60 * 1000); // Chờ một khoảng thời gian trước khi thử lại
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // Khôi phục trạng thái gián đoạn
			}
		}

		// Trả về trạng thái thành công của quá trình crawl
		return crawlSuccess;
	}

	// Phương thức xử lý lỗi để giảm thiểu trùng lặp mã
	private void handleError(Config readyConfig, Log log, String message, Exception e) {
		//17. 2 Update trạng thái = FAILURE_EXTRACTvà thời gian update = thời gian hiện tại của log lại
		String stackTrace = Arrays.toString(e.getStackTrace());
		log.setLogLevel(LogLevel.ERROR);
		log.setStackTrace(stackTrace);
		log.setStatus(Status.FAILURE_EXTRACT);
		log.setErrorMessage(message);
		log.setUpdateTime(LocalDateTime.now());
		logService.updateLog(log);
		//17.3 Gửi email thông báo tới các địa chỉ được chỉ định trong config lỗi tương ứng
		emailService.sendFailureEmail(readyConfig.getNotificationEmails(), message);
		System.err.println(message);
	}
}

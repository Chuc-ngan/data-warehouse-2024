USE control;
DROP PROCEDURE IF EXISTS transform_and_cleaning_data;

delimiter //
CREATE PROCEDURE transform_and_cleaning_data() 
BEGIN
	
	-- Khai báo biến record_count
	DECLARE record_count INT;

	-- Xoá bảng tạm nếu tồn tại
	DROP TEMPORARY TABLE IF EXISTS staging_combined;
	DROP TEMPORARY TABLE IF EXISTS temp_product;
	
	-- xóa toàn bộ dữ liệu bảng bảng product_staging trong staging db trước khi lọc dữ liệu
   TRUNCATE TABLE staging.product_staging;
	
	-- kiểm trang tình trạng lần crawl gần nhất trong bảng logs
	SELECT COUNT(*) INTO record_count
		FROM `control`.`logs` 
		WHERE `control`.`logs`.`status`='SUCCESS_LOAD_DATA' 
			AND DATE(`control`.`logs`.create_time)=CURDATE();
	
	SET @log_id=(SELECT l.id 
						FROM `control`.`logs` l
						WHERE l.`status`='SUCCESS_LOAD_DATA' 
							AND DATE(l.create_time)=CURDATE());
	
	-- Nếu như không có record nào trong bảng kết quả
	IF record_count = 0 THEN 
		-- In ra thông báo nếu không có record nào
		SELECT 'Không có record nào hết' AS `error`;
		-- SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No record found';
	ELSE 
		-- Tạo bảng tạm staging_combined để chứa dữ liệu từ bảng staging_tiki
		CREATE TEMPORARY TABLE staging_combined (
			`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
			`product_id` BIGINT DEFAULT NULL,
			`sku` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`product_name` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
			`price` DECIMAL(15,2) DEFAULT NULL,
			`original_price` DECIMAL(15,2) DEFAULT NULL,
			`brand_name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`discount_value` DECIMAL(15,2) DEFAULT NULL,
			`thumbnail_url` VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`short_description` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
			`image_urls` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
			`color_options` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`size_options` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`rating_average` DECIMAL(3,2) DEFAULT NULL,
			`review_count` INT DEFAULT NULL,
			`discount_rate` DECIMAL(5,2) DEFAULT NULL,
			`quantity_sold` INT DEFAULT NULL,
			`url_key` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`url_path` VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`short_url` VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`product_type` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
			`created_at` TIMESTAMP NULL DEFAULT NULL
		); SET SESSION group_concat_max_len = 1000000;
	 
		-- loại bỏ dữ liệu dữ thừa và dữ liệu bị null và 
		-- insert record vào bảng tạm từ staging_tiki_1 
		INSERT INTO staging_combined (
			product_id, sku, product_name, price, original_price, brand_name, discount_value,
			thumbnail_url, short_description, image_urls, color_options, size_options,
			rating_average, review_count, discount_rate, quantity_sold, url_key,
			url_path, short_url, product_type, created_at
		)
		SELECT 
			product_id, 
			sku,
			product_name,
			
			-- Xử lý giá (price) nếu có định dạng số kèm "đ"
			CASE
				WHEN price IS NULL OR price = '' THEN NULL
				WHEN TRIM(price) REGEXP '^[0-9]+([.,][0-9]{1,3})?$' THEN
					CAST(REPLACE(REPLACE(TRIM(price), ',', ''), '.', '.') AS DECIMAL(15,2)) -- Giữ nguyên giá trị không có "đ"
				WHEN TRIM(price) REGEXP '^[0-9]+([.,][0-9]{1,3})?\\s*đ$' THEN  -- Kiểm tra định dạng số kèm "đ"
				   CAST(REPLACE(REPLACE(REPLACE(TRIM(price), 'đ', ''), ',', ''), '.', '.') AS DECIMAL(15,2))  
				ELSE  
					NULL  -- Các trường hợp không xác định, đặt thành NULL
			END AS price,
			
			-- Xử lý giá gốc (original_price) nếu có định dạng số kèm "đ"
			CASE
				WHEN original_price IS NULL OR original_price = '' THEN NULL
				WHEN TRIM(original_price) REGEXP '^[0-9]+([.,][0-9]{1,3})?$' THEN
					CAST(REPLACE(REPLACE(TRIM(original_price), ',', ''), '.', '.') AS DECIMAL(15,2)) -- Giữ nguyên giá trị không có "đ"
				WHEN TRIM(original_price) REGEXP '^[0-9]+([.,][0-9]{1,3})?\\s*đ$' THEN  -- Kiểm tra định dạng số kèm "đ"
				   CAST(REPLACE(REPLACE(REPLACE(TRIM(original_price), 'đ', ''), ',', ''), '.', '.') AS DECIMAL(15,2))
				ELSE  
					NULL  -- Các trường hợp không xác định, đặt thành NULL
			END AS original_price,
	 	
			-- Lọc tên thương hiệu (brand_name), nếu là NULL thì trả về giá trị mặc định
			IFNULL(brand_name, 'Chưa có') AS brand_name,
	 	
			-- Lọc giá trị discount_value (nếu NULL hoặc không hợp lệ, trả về 0)
			CASE 
				WHEN discount_value IS NULL THEN 0 
				WHEN discount_value < 0 THEN 0 
				ELSE discount_value 
			END AS discount_value,
			
			thumbnail_url,
			
			-- Lọc short_description (nếu NULL, trả về 'Không có thông tin mô tả')
			IFNULL(short_description, 'Không có thông tin mô tả') AS short_description,
			
			image_urls,
			
			-- Lọc color_options (nếu NULL, trả về 'Không có màu sắc khác')
			IFNULL(color_options, 'Không có màu sắc khác') AS color_options,
	
			-- Lọc size_options (nếu NULL, trả về 'Không có kích thước khác')
			CASE 
				WHEN size_options IS NULL THEN 'Không có kích thước khác'
				ELSE 
					TRIM(
						REGEXP_REPLACE(
							REGEXP_REPLACE(
								REGEXP_REPLACE(
									size_options, 
									'\\s*\\([^\\)]*\\)', -- Bỏ nội dung trong ngoặc tròn, bao gồm cả dấu ngoặc
	                    		''	
								),
								'\\s*US\\b', -- Bỏ "US" và khoảng trắng liền trước
								''
							),
							'\\s*,\\s*', -- Thay dấu phẩy và khoảng trắng giữa các số
                		';' -- Thành dấu chấm phẩy và khoảng trắng
						)
					)
			END AS size_options,
			
			-- Lọc rating_average (nếu NULL, trả về 0)
			CASE 
				WHEN rating_average IS NULL OR rating_average < 0 THEN 0 
				ELSE rating_average 
			END AS rating_average,
			
			-- Lọc review_count (nếu NULL hoặc không hợp lệ, trả về 0)
			CASE 
				WHEN review_count IS NULL OR review_count < 0 
					THEN 0 
				ELSE review_count 
			END AS review_count,
			
			-- Lọc discount_rate (nếu NULL hoặc không hợp lệ, trả về 0)
			CASE 
				WHEN discount_rate IS NULL OR discount_rate < 0 
					THEN 0 
				ELSE discount_rate 
			END AS discount_rate,
			
			-- Lọc quantity_sold (nếu NULL hoặc không hợp lệ, trả về 0)
			CASE 
				WHEN quantity_sold IS NULL OR quantity_sold < 0 
					THEN 0 
				ELSE quantity_sold 
			END AS quantity_sold,
			
			-- Lọc url_key (nếu NULL, trả về 'No URL key')
			IFNULL(url_key, 'No URL key') AS url_key,
			
			-- Lọc url_path (nếu NULL, trả về 'No URL path')
			IFNULL(url_path, 'No URL path') AS url_path,
			
			-- Lọc short_url (nếu NULL, trả về 'No short URL')
			IFNULL(short_url, 'No short URL') AS short_url,
			
			-- Lọc product_type (nếu NULL, trả về 'No product type')
			IFNULL(product_type, 'No product type') AS product_type,
			
			-- Lọc created_at (nếu NULL, trả về ngày hiện tại)
			IFNULL(created_at, NOW()) AS created_at
		FROM staging.staging_tiki_1;
		
		-- loại bỏ dữ liệu dữ thừa và dữ liệu bị null và 
		-- insert record vào bảng tạm từ staging_tiki_2
		INSERT INTO staging_combined (
			product_id, sku, product_name, price, original_price, brand_name, discount_value,
			thumbnail_url, short_description, image_urls, color_options, size_options,
			rating_average, review_count, discount_rate, quantity_sold, url_key,
			url_path, short_url, product_type, created_at
		)
		SELECT 
			product_id, 
			sku,
			product_name,
			
			-- Xử lý giá (price) nếu có định dạng số kèm "đ"
			CASE
				WHEN price IS NULL OR price = '' THEN NULL
				WHEN TRIM(price) REGEXP '^[0-9]+([.,][0-9]{1,3})?$' THEN
					CAST(REPLACE(REPLACE(TRIM(price), ',', ''), '.', '.') AS DECIMAL(15,2)) -- Giữ nguyên giá trị không có "đ"
				WHEN TRIM(price) REGEXP '^[0-9]+([.,][0-9]{1,3})?\\s*đ$' THEN  -- Kiểm tra định dạng số kèm "đ"
				   CAST(REPLACE(REPLACE(REPLACE(TRIM(price), 'đ', ''), ',', ''), '.', '.') AS DECIMAL(15,2))  
				ELSE  
					NULL  -- Các trường hợp không xác định, đặt thành NULL
			END AS price,
			
			-- Xử lý giá gốc (original_price) nếu có định dạng số kèm "đ"
			CASE
				WHEN original_price IS NULL OR original_price = '' THEN NULL
				WHEN TRIM(original_price) REGEXP '^[0-9]+([.,][0-9]{1,3})?$' THEN
					CAST(REPLACE(REPLACE(TRIM(original_price), ',', ''), '.', '.') AS DECIMAL(15,2)) -- Giữ nguyên giá trị không có "đ"
				WHEN TRIM(original_price) REGEXP '^[0-9]+([.,][0-9]{1,3})?\\s*đ$' THEN  -- Kiểm tra định dạng số kèm "đ"
				   CAST(REPLACE(REPLACE(REPLACE(TRIM(original_price), 'đ', ''), ',', ''), '.', '.') AS DECIMAL(15,2))
				ELSE  
					NULL  -- Các trường hợp không xác định, đặt thành NULL
			END AS original_price,
	 	
			-- Lọc tên thương hiệu (brand_name), nếu là NULL thì trả về giá trị mặc định
			IFNULL(brand_name, 'Chưa có') AS brand_name,
	 	
			-- Lọc giá trị discount_value (nếu NULL hoặc không hợp lệ, trả về 0)
			CASE 
				WHEN discount_value IS NULL THEN 0 
				WHEN discount_value < 0 THEN 0 
				ELSE discount_value 
			END AS discount_value,
			
			thumbnail_url,
			
			-- Lọc short_description (nếu NULL, trả về 'Không có thông tin mô tả')
			IFNULL(short_description, 'Không có thông tin mô tả') AS short_description,
			
			image_urls,
			
			-- Lọc color_options (nếu NULL, trả về 'Không có màu sắc khác')
			IFNULL(color_options, 'Không có màu sắc khác') AS color_options,
	
			-- Lọc size_options (nếu NULL, trả về 'Không có kích thước khác')
			CASE 
				WHEN size_options IS NULL THEN 'Không có kích thước khác'
				ELSE 
					TRIM(
						REGEXP_REPLACE(
							REGEXP_REPLACE(
								REGEXP_REPLACE(
									size_options, 
									'\\s*\\([^\\)]*\\)', -- Bỏ nội dung trong ngoặc tròn, bao gồm cả dấu ngoặc
	                    		''	
								),
								'\\s*US\\b', -- Bỏ "US" và khoảng trắng liền trước
								''
							),
							'\\s*,\\s*', -- Thay dấu phẩy và khoảng trắng giữa các số
                		';' -- Thành dấu chấm phẩy và khoảng trắng
						)
					)
			END AS size_options,
			
			-- Lọc rating_average (nếu NULL, trả về 0)
			CASE 
				WHEN rating_average IS NULL OR rating_average < 0 THEN 0 
				ELSE rating_average 
			END AS rating_average,
			
			-- Lọc review_count (nếu NULL hoặc không hợp lệ, trả về 0)
			CASE 
				WHEN review_count IS NULL OR review_count < 0 
					THEN 0 
				ELSE review_count 
			END AS review_count,
			
			-- Lọc discount_rate (nếu NULL hoặc không hợp lệ, trả về 0)
			CASE 
				WHEN discount_rate IS NULL OR discount_rate < 0 
					THEN 0 
				ELSE discount_rate 
			END AS discount_rate,
			
			-- Lọc quantity_sold (nếu NULL hoặc không hợp lệ, trả về 0)
			CASE 
				WHEN quantity_sold IS NULL OR quantity_sold < 0 
					THEN 0 
				ELSE quantity_sold 
			END AS quantity_sold,
			
			-- Lọc url_key (nếu NULL, trả về 'No URL key')
			IFNULL(url_key, 'No URL key') AS url_key,
			
			-- Lọc url_path (nếu NULL, trả về 'No URL path')
			IFNULL(url_path, 'No URL path') AS url_path,
			
			-- Lọc short_url (nếu NULL, trả về 'No short URL')
			IFNULL(short_url, 'No short URL') AS short_url,
			
			-- Lọc product_type (nếu NULL, trả về 'No product type')
			IFNULL(product_type, 'No product type') AS product_type,
			
			-- Lọc created_at (nếu NULL, trả về ngày hiện tại)
			IFNULL(created_at, NOW()) AS created_at
		FROM staging.staging_tiki_2;
	
		CREATE TEMPORARY TABLE temp_product AS
		SELECT 
			sc.product_id,
			sc.sku,
			sc.product_name,
			sc.price,
			sc.original_price,
			sc.brand_name,
			sc.discount_value,
			sc.thumbnail_url,
			sc.short_description,
			sc.image_urls,
			sc.color_options,
			sc.size_options,
			sc.rating_average,
			sc.review_count,
			sc.discount_rate,
			sc.quantity_sold,
			sc.url_key,
			sc.url_path,
			sc.short_url,
			sc.product_type,
			sc.created_at
		FROM 
			staging_combined AS sc;
											
		-- kiểm tra ngày hôm nay có tồn tại trong bảng date_dim không
		SET @date_exits = (SELECT COUNT(*) 
								FROM staging.date_dim dd
								WHERE dd.full_date = CURRENT_DATE);
	
		-- nếu chưa có record về ngày đó trong product_staging
		IF(@date_exits = 0) THEN 
			
			-- lấy ra date_sk cao nhất trong bảng
			SET @max_date_sk = (SELECT IFNULL(MAX(date_sk), 0) FROM staging.date_dim);
		
			-- thêm record vào bảng date_dim theo ngày hiện tại 
			INSERT INTO staging.date_dim (
				date_sk,
				full_date,
				day_since_2024,
				month_since_2024,
				day_of_week,
				calendar_month,
				calendar_year,
				calendar_year_month,
				day_of_month,
				day_of_year,
				week_of_year_sunday,
				year_week_sunday,
				week_sunday_start,
				week_of_year_monday,
				year_week_monday,
				week_monday_start,
				holiday,
				day_type
			)
			VALUES (
				@max_date_sk + 1, 											-- date_sk tự động tăng
				CURRENT_DATE,                                      -- full_date: ngày hiện tại
				DATEDIFF(CURRENT_DATE, '2024-01-01'),              -- day_since_2024: số ngày từ đầu năm 2024
				TIMESTAMPDIFF(MONTH, '2024-01-01', CURRENT_DATE),  -- month_since_2024: số tháng từ đầu năm 2024
				DAYNAME(CURRENT_DATE),                             -- day_of_week: tên ngày trong tuần
				MONTHNAME(CURRENT_DATE),                           -- calendar_month: tên tháng
				YEAR(CURRENT_DATE),                                -- calendar_year: năm hiện tại
				DATE_FORMAT(CURRENT_DATE, '%Y-%m'),                -- calendar_year_month: năm-tháng
				DAY(CURRENT_DATE),                                 -- day_of_month: ngày trong tháng
				DAYOFYEAR(CURRENT_DATE),                           -- day_of_year: ngày trong năm
				WEEK(CURRENT_DATE, 7),                             -- week_of_year_sunday: tuần tính từ Chủ Nhật
				DATE_FORMAT(CURRENT_DATE, '%X-%V'),                -- year_week_sunday: năm-tuần (Chủ Nhật bắt đầu)
				SUBDATE(CURRENT_DATE, WEEKDAY(CURRENT_DATE) + 1),  -- week_sunday_start: ngày bắt đầu tuần (Chủ Nhật)
				WEEK(CURRENT_DATE, 1),                             -- week_of_year_monday: tuần tính từ Thứ Hai
				DATE_FORMAT(CURRENT_DATE, '%X-%V'),                -- year_week_monday: năm-tuần (Thứ Hai bắt đầu)
				SUBDATE(CURRENT_DATE, WEEKDAY(CURRENT_DATE)),      -- week_monday_start: ngày bắt đầu tuần (Thứ Hai)
				'No',                                              -- holiday: ngày nghỉ lễ (tùy ý)
				IF(DAYOFWEEK(CURRENT_DATE) IN (1, 7), 'Weekend', 'Weekday') -- day_type: loại ngày (cuối tuần/ngày thường)
			);
		END IF;
	
		-- lấy ra date_sk mới nhất trong bảng date_dim 
		SET @max_date_sk = (SELECT MAX(dd.date_sk) FROM staging.date_dim dd);
	
		-- Cập nhật nhiều sản phẩm từ temp_product vào bảng product_staging
		INSERT INTO staging.product_staging(
				product_id,
				sku,
				product_name,
				price,
				original_price,
				brand_name,
				discount_value,
				thumbnail_url,
				short_description,
				image_urls,
				color_options,
				size_options,
				rating_average,
				review_count,
				discount_rate,
				quantity_sold,
				url_key,
				url_path,
				short_url,
				product_type,
				created_at,
				id_date
			)
			SELECT 
				sc.product_id,
				sc.sku,
				sc.product_name,
				sc.price,
				sc.original_price,
				sc.brand_name,
				sc.discount_value,
				sc.thumbnail_url,
				sc.short_description,
				sc.image_urls,
				sc.color_options,
				sc.size_options,
				sc.rating_average,
				sc.review_count,
				sc.discount_rate,
				sc.quantity_sold,
				sc.url_key,
				sc.url_path,
				sc.short_url,
				sc.product_type,
				sc.created_at,
				@max_date_sk -- Giá trị khóa thời gian
			FROM staging_combined sc;
			
		-- UPDATE control.`logs`
-- 		SET `status` = 'SUCCESS_TRANSFORM' 
-- 		WHERE id = @log_id;
		
		-- Trả về kết quả từ bảng tạm staging_combined để kiểm tra
		SELECT * FROM staging.product_staging;
			
	END IF;
	
END //
delimiter ;

-- Gọi stored procedure để thực hiện 
CALL transform_and_cleaning_data;
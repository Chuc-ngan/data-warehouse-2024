DELIMITER $$

CREATE DEFINER=`root`@`localhost` PROCEDURE get_load_config_data_mul()
BEGIN
    SELECT 
        l.destination_path AS file_path, 
        c.`delimiter` AS fields_terminated, 
        c.file_type AS optionally_enclosed, 
        c.created_by AS lines_terminated, 
        1 AS ignore_row, 
        c.columns AS stg_fields, 
        l.id AS log_id, 
        c.tables AS table_staging, 
        l.status AS status, 
        l.create_time AS create_time,
        l.id_config AS id_config
    FROM 
        control.logs l
    JOIN 
        control.config c 
    ON 
        l.id_config = c.id
    WHERE 
        l.status = 'SUCCESS_EXTRACT'
        AND DATE(l.create_time) = CURDATE()
    ORDER BY 
        l.id DESC
    LIMIT 10;
END$$

DELIMITER ;
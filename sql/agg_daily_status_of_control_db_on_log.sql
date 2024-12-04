USE control;
DROP PROCEDURE IF EXISTS agg_daily_status_of_control_db_on_log;

DELIMITER //
CREATE PROCEDURE agg_daily_status_of_control_db_on_log(
	IN input_config_id INT 
) 
BEGIN
	SELECT * 
	FROM control.`logs`
	WHERE control.`logs`.id_config=input_config_id
	ORDER BY control.`logs`.id DESC
	LIMIT 5;
END //
DELIMITER ;

CALL agg_daily_status_of_control_db_on_log(2);
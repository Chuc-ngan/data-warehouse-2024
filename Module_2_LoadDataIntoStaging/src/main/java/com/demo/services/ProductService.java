package com.demo.services;

import javax.sql.DataSource;
import java.sql.Connection;

public interface ProductService {

	// load csv vào database staging
	public void importCSV(String id);

	// tạo bảng
	public void createTable(DataSource dataSource);

	// check db trong staging là dữ liệu cũ hay mới
	public void checkOldData();

	public void createTables(DataSource stagingDataSource);
	
}

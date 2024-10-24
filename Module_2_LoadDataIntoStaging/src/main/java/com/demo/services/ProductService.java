package com.demo.services;

import javax.sql.DataSource;
import java.sql.Connection;

public interface ProductService {

	public void importCSV(String filePath);

	public void createTable(DataSource dataSource);
	
}

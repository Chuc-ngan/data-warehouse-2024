package com.nlu.app.helper;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {

    public List<String> readProductIdsFromCsv(String filePath) {
        List<String> productIds = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                // Giả sử ID sản phẩm nằm ở cột đầu tiên
                productIds.add(nextLine[0]);
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }

        return productIds;
    }

    public static void main(String[] args) {
        CsvReader csvReader = new CsvReader();
        List<String> productIds = csvReader.readProductIdsFromCsv("D:\\workspace\\Project\\DataWarehouse\\data-warehouse-2024\\datawarehouse-crawl\\product_id_ncds.csv");
        System.out.println("Product IDs: " + productIds);
    }
}

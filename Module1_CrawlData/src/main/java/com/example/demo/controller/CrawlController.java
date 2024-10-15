package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.crawler.CrawlService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private final CrawlService crawlService;

    @Autowired
    public CrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @GetMapping("/products")
    public List<Product> crawlProducts() throws InterruptedException, JsonProcessingException {
        List<String> productIds = Arrays.asList("75031062", "216098291", "163806087");
        return crawlService.crawlProducts(productIds);
    }

}
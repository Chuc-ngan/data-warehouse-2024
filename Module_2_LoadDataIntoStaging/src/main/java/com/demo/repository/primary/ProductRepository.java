package com.demo.repository.primary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.entities.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>{

}

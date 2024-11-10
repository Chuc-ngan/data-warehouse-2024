package com.demo.repository;

import com.demo.entities.Config;
import com.demo.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<Config, String>  {
}

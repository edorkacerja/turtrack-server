package com.turtrack.server.repository.turtrack;

import com.turtrack.server.model.turtrack.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByActiveTrue();
    boolean existsByName(String name);
}
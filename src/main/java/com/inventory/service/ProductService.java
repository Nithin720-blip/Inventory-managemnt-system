package com.inventory.service;

import com.inventory.dao.ProductDAO;
import com.inventory.model.Product;
import java.sql.SQLException;
import java.util.List;

public class ProductService {
    private final ProductDAO productDAO;
    public ProductService() { this(new ProductDAO()); }
    public ProductService(ProductDAO productDAO) { this.productDAO=productDAO; }

    public long create(Product product) throws SQLException {
        validate(product);
        return productDAO.create(product);
    }
    public Product findById(long id) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("Product ID must be positive");
        Product product=productDAO.findById(id);
        if(product==null) throw new IllegalArgumentException("Product not found");
        return product;
    }
    public List<Product> findAll() throws SQLException { return productDAO.findAll(); }
    public boolean update(Product product) throws SQLException { validate(product); if(product.getId()<=0) throw new IllegalArgumentException("Product ID must be positive"); return productDAO.update(product); }
    public boolean delete(long id) throws SQLException { if(id<=0) throw new IllegalArgumentException("Product ID must be positive"); return productDAO.delete(id); }
    private void validate(Product product) {
        if(product==null) throw new IllegalArgumentException("Product is required");
        if(product.getSku()==null || product.getSku().isBlank()) throw new IllegalArgumentException("Product SKU is required");
        if(product.getName()==null || product.getName().isBlank()) throw new IllegalArgumentException("Product name is required");
        if(product.getPrice()==null || product.getPrice().signum()<0) throw new IllegalArgumentException("Product price must be zero or greater");
    }
}

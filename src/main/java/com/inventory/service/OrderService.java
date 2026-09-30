package com.inventory.service;

import com.inventory.dao.OrderDAO;
import com.inventory.dao.OrderItemDAO;
import com.inventory.dao.ProductDAO;
import com.inventory.dao.UserDAO;
import com.inventory.model.Order;
import com.inventory.model.OrderItem;
import com.inventory.model.OrderStatus;
import com.inventory.util.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrderService {
    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;

    public OrderService() { this(new OrderDAO(), new OrderItemDAO(), new UserDAO(), new ProductDAO()); }
    public OrderService(OrderDAO orderDAO, OrderItemDAO orderItemDAO, UserDAO userDAO) { this(orderDAO, orderItemDAO, userDAO, new ProductDAO()); }
    public OrderService(OrderDAO orderDAO, OrderItemDAO orderItemDAO, UserDAO userDAO, ProductDAO productDAO) { this.orderDAO=orderDAO; this.orderItemDAO=orderItemDAO; this.userDAO=userDAO; this.productDAO=productDAO; }

    public Order createOrder(long userId, List<OrderItem> items) throws SQLException {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Order must contain at least one item");
        for (OrderItem item : items) {
            if (item.getQuantity() <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        try (Connection c=DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (!userDAO.exists(c,userId)) throw new IllegalArgumentException("User not found");
                BigDecimal total = BigDecimal.ZERO;
                for (OrderItem item : items) {
                    com.inventory.model.Product product = productDAO.findById(c, item.getProductId());
                    if (product == null) throw new IllegalArgumentException("Product not found");
                    item.setUnitPrice(product.getPrice());
                    total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
                Order order = new Order(0,userId,total,OrderStatus.PENDING,null);
                long orderId=orderDAO.create(c,order);
                for(OrderItem item:items) {
                    item.setOrderId(orderId);
                    orderItemDAO.create(c,item);
                }
                c.commit();
                order.setId(orderId);
                return order;
            } catch(SQLException | RuntimeException exception) {
                try { c.rollback(); } catch(SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                throw exception;
            }
        }
    }

    public List<Order> getAllOrders() throws SQLException {
        return orderDAO.findAll();
    }

    public List<OrderItem> getOrderItems(long orderId) throws SQLException {
        return orderItemDAO.findByOrderId(orderId);
    }
}


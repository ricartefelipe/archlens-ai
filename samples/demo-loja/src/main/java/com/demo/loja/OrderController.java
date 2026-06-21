package com.demo.loja;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

/** Controller acoplado ao JDBC — smell intencional para demo ArchLens. */
public class OrderController {

    private final String jdbcUrl = System.getenv().getOrDefault(
            "DATABASE_URL", "jdbc:postgresql://localhost:5432/loja");

    public UUID createOrder(String customerEmail, double total) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "loja", "loja")) {
            UUID customerId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            conn.createStatement().execute(
                    "INSERT INTO customers (id, email) VALUES ('" + customerId + "','" + customerEmail + "')");
            conn.createStatement().execute(
                    "INSERT INTO orders (id, customer_id, total) VALUES ('"
                            + orderId + "','" + customerId + "'," + total + ")");
            return orderId;
        }
    }
}

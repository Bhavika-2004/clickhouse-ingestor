package com.example.clickhouseingestor.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ClickHouseClient {
    public static Connection connect(String host, int port, String database, String jwtToken) throws SQLException {
        String url = String.format("jdbc:clickhouse://%s:%d/%s", host, port, database);
        return DriverManager.getConnection(url, "default", jwtToken); // JWT as password
    }
}

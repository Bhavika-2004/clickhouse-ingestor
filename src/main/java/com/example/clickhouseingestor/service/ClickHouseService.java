package com.example.clickhouseingestor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClickHouseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insertRow(int id, String name, int age) {
        String sql = "INSERT INTO people (id, name, age) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, id, name, age);
    }
}

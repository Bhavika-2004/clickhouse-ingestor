package com.example.clickhouseingestor.controller;

import com.example.clickhouseingestor.client.ClickHouseClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class IngestorController {

    // Step 1: Upload CSV → Return headers
    @PostMapping("/upload-csv")
    public ResponseEntity<List<String>> handleCsvUpload(@RequestParam("file") MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            String[] columns = headerLine.split(",");
            List<String> cleanedHeaders = Arrays.stream(columns).map(String::trim).collect(Collectors.toList());

            return ResponseEntity.ok(cleanedHeaders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Collections.emptyList());
        }
    }

    // Create table based on user input
    @PostMapping("/create-table")
    public ResponseEntity<?> createCustomTable(@RequestBody Map<String, String> body) {
        String host = body.get("clickhouseHost");
        int port = Integer.parseInt(body.get("clickhousePort"));
        String db = body.get("clickhouseDb");
        String jwt = body.get("jwt");
        String tableName = body.get("tableName");
        String columns = body.get("columns");
    
        try (Connection conn = ClickHouseClient.connect(host, port, db, jwt);
             Statement stmt = conn.createStatement()) {
    
            String sanitizedTable = tableName.replaceAll("[^a-zA-Z0-9_]", "_");
            String sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s) ENGINE = MergeTree() ORDER BY tuple()", sanitizedTable, columns);
    
            stmt.execute(sql);
            return ResponseEntity.ok(Map.of("message", "Table created: " + sanitizedTable));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    
    // Step 2: Insert selected columns into given table
    @PostMapping("/submit-columns")
    public ResponseEntity<Map<String, String>> handleSelectedColumns(
            @RequestParam("file") MultipartFile file,
            @RequestParam("columns") String columnsJson,
            @RequestParam("clickhouseHost") String host,
            @RequestParam("clickhousePort") int port,
            @RequestParam("clickhouseDb") String database,
            @RequestParam("jwt") String jwt,
            @RequestParam("tableName") String tableName
    ) {
        try {
            List<String> selectedColumns = new ObjectMapper().readValue(columnsJson, List.class);

            try (Connection conn = ClickHouseClient.connect(host, port, database, jwt);
                 Statement stmt = conn.createStatement();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

                String[] allColumns = reader.readLine().split(",");
                for (int i = 0; i < allColumns.length; i++) {
                    allColumns[i] = allColumns[i].trim();
                }

                List<Integer> selectedIndexes = new ArrayList<>();
                for (String col : selectedColumns) {
                    for (int i = 0; i < allColumns.length; i++) {
                        if (allColumns[i].equals(col)) {
                            selectedIndexes.add(i);
                            break;
                        }
                    }
                }

                int insertedCount = 0;
                String line;

                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    List<String> row = new ArrayList<>();
                    for (int index : selectedIndexes) {
                        row.add("'" + (index < values.length ? values[index].replace("'", "''").trim() : "") + "'");
                    }

                    String insertSql = String.format(
                            "INSERT INTO %s (%s) VALUES (%s)",
                            tableName,
                            String.join(",", selectedColumns),
                            String.join(",", row)
                    );

                    stmt.execute(insertSql);
                    insertedCount++;
                }

                return ResponseEntity.ok(Map.of(
                        "message", "Ingestion complete to ClickHouse!",
                        "rowsInserted", String.valueOf(insertedCount))
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Ingestion failed: " + e.getMessage()));
        }
    }

    // Step 3: Export selected columns
    @GetMapping("/export-csv")
public ResponseEntity<String> exportCsv(
    @RequestParam String clickhouseHost,
    @RequestParam int clickhousePort,
    @RequestParam String clickhouseDb,
    @RequestParam String jwt,
    @RequestParam String table,
    @RequestParam(name = "columns") List<String> columns
) {
    try (Connection conn = ClickHouseClient.connect(clickhouseHost, clickhousePort, clickhouseDb, jwt);
         Statement stmt = conn.createStatement()) {

        String query = String.format("SELECT %s FROM %s", String.join(",", columns), table);
        ResultSet rs = stmt.executeQuery(query);

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(String.join(",", columns)).append("\n");

        while (rs.next()) {
            for (int i = 0; i < columns.size(); i++) {
                String val = rs.getString(i + 1);
                csvBuilder.append(val != null ? val.replace(",", " ") : "");
                if (i < columns.size() - 1) csvBuilder.append(",");
            }
            csvBuilder.append("\n");
        }

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=" + table + "_export.csv")
            .header("Content-Type", "text/csv")
            .body(csvBuilder.toString());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body("Export failed: " + e.getMessage());
    }
}

    // Step 4: View ALL table data (e.g. for previewing)
    @GetMapping("/all-data")
    public ResponseEntity<?> fetchAllData(
            @RequestParam("clickhouseHost") String host,
            @RequestParam("clickhousePort") int port,
            @RequestParam("clickhouseDb") String database,
            @RequestParam("jwt") String jwt
    ) {
        try (Connection conn = ClickHouseClient.connect(host, port, database, jwt);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM ingested_data")) {

            List<Map<String, String>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnName(i), rs.getString(i));
                }
                rows.add(row);
            }

            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    // Step 5: View specific table data (with column filter)
    @GetMapping("/view-data")
    public ResponseEntity<?> viewData(
            @RequestParam String clickhouseHost,
            @RequestParam int clickhousePort,
            @RequestParam String clickhouseDb,
            @RequestParam String jwt,
            @RequestParam String table,
            @RequestParam List<String> columns
    ) {
        try (Connection conn = ClickHouseClient.connect(clickhouseHost, clickhousePort, clickhouseDb, jwt);
             Statement stmt = conn.createStatement()) {

            String query = String.format("SELECT %s FROM %s LIMIT 100", String.join(",", columns), table);
            ResultSet rs = stmt.executeQuery(query);

            List<Map<String, String>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < columns.size(); i++) {
                    row.put(columns.get(i), rs.getString(i + 1));
                }
                rows.add(row);
            }

            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

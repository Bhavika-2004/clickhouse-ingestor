# ClickHouse Ingestor Web App

A lightweight full-stack web application that allows users to:

- Upload a CSV file
- Select specific columns for ingestion
- Create a ClickHouse table with custom schema
- Insert selected data into the table
- Preview data from the table
- Export selected columns as a downloadable CSV

## Tech Stack

- **Frontend**: HTML, CSS, JavaScript (vanilla)
- **Backend**: Java Spring Boot
- **Database**: ClickHouse

---

## Features

### 1. Connect to ClickHouse
Users provide ClickHouse connection details (host, port, database name, and JWT/password).

### 2. Create Table
Allows creating a table by specifying a table name and schema (e.g., `id Int32, name String`).

### 3. Upload CSV & Select Columns
- Uploads a CSV file
- Displays headers as checkboxes
- Lets user choose which columns to ingest

### 4. Ingest Data
- Data is inserted into the user-defined table based on selected columns
- Additional sanitization and quoting are handled automatically

### 5. View Table Data
Preview specific columns of a ClickHouse table (first 100 rows).

### 6. Export CSV
- Export selected columns from a ClickHouse table
- Downloads as a `.csv` file

---

## Setup Instructions

### Prerequisites
- Java 11 or later
- Maven
- ClickHouse server running (can be in Docker)

### Backend Setup
```bash
# Clone the project
cd clickhouse-ingestor

# Build the backend
mvn clean install

# Run the Spring Boot application
mvn spring-boot:run
```

### ClickHouse Docker Example
```bash
docker run -d --name clickhouse-server \
  -p 8123:8123 \
  -p 9000:9000 \
  clickhouse/clickhouse-server
```

To access the CLI:
```bash
docker exec -it clickhouse-server clickhouse-client
```

### Frontend Usage
Open the `index.html` file in a browser. You can place it inside `resources/static/` or serve it via a simple web server.

---

## API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/upload-csv` | Upload CSV and return headers |
| POST | `/api/create-table` | Create a ClickHouse table |
| POST | `/api/submit-columns` | Insert selected columns into a table |
| GET | `/api/view-data` | View selected columns (limit 100) |
| GET | `/api/export-csv` | Export columns as downloadable CSV |

---

## Example Table Definition
```
id Int32, name String, age Int32
```

---

## Author
Developed as part of a software engineering internship assignment.


---

# Data Integration Platform

## Project Overview

This project demonstrates a **data integration pipeline** that reads order data from **Kafka**, processes the data, and performs **upsert operations** into an **Iceberg table**. It serves as an example implementation of a modular and scalable real-time data pipeline using **Apache Spark**, **Kafka**, and **Iceberg**.

### Functional Modules

1. **Data Source Module (`datasource`)**
   - Facilitates data ingestion from various sources:
      - **Kafka**: Reads real-time streaming order data via `KafkaSource`.

2. **Data Processing Module (`processing`)**
   - Processes the raw order data to ensure quality and apply business rules:
      - **`DataCleaner`**: Cleans and formats incoming order data for consistency.
      - **`DataTransformer`**: Applies business transformation logic to the cleaned data.

3. **Data Sink Module (`sink`)**
   - Writes the processed data into the target **Iceberg** table with upsert functionality:
      - **Iceberg**: Integrates with the data lake via `OrderSink`, supporting efficient upsert operations.

4. **Configuration and Utility Modules**
   - **Configuration Management (`config`)**: Manages global configuration files for seamless setup and operation.
   - **Utilities (`utils`)**:
      - **`KafkaUtil`**: Simplifies Kafka operations for reading order data.
      - **`IcebergUtil`**: Provides utilities for Iceberg table management, including upsert operations.
      - **`PropertiesUtil`**: Handles loading and parsing of configuration files.

5. **Job Execution Module (`job`)**
   - **`StreamingJobExecutor`**: Executes the end-to-end pipeline:
      - Ingests data from Kafka.
      - Cleans and transforms the data.
      - Performs upserts into the Iceberg table.

6. **Data Models (`bean`)**
   - **`Order`**: Represents the structure of an order entity.
   - **`OrderSchema`**: Defines the schema for order data, ensuring consistency throughout the pipeline.

7. **Resources (`resources`)**
   - Includes essential configuration files and example datasets for quick setup and testing:
      - **`config.properties`**: Specifies global configurations for Kafka, Iceberg, and Spark.
      - **`log4j2.properties`**: Configures logging for the application.
      - **`orders.json`**: Sample order data for testing and demonstration.

### Use Case

This example demonstrates how to:
1. Ingest order data from Kafka in real time.
2. Process and clean the data to meet business requirements.
3. Write the processed data into an Iceberg table with **upsert** functionality, ensuring that changes to existing records are handled efficiently.

---
# Setup and Run Integration tests
1. Configure Kafka
   Ensure Kafka is running and accessible. Update src/main/resources/config.properties with the correct Kafka configuration:

properties
Copy code
kafka.bootstrap-servers = localhost:9092
2. Build the Project
   Build the project using Maven:

bash
```
mvn clean install
```
3. Run the Integration Test Suite
   Execute the test suite using Maven:

bash
```
mvn test
```
Test Cases
The test suite includes the following cases:

* Test Insert 2 New Orders: Validates that two new orders are successfully inserted into the Iceberg table.
* Test Insert One Duplicated Order: Ensures duplicate orders are not reinserted.
* Test Update the Status of One Order: Verifies that the order status is updated correctly.
* Test Event Sequential Consistency: Ensures the sequence of events in the Iceberg table is consistent.
* Test Data Integrity: Validates the integrity of specific fields in the inserted records.
  Logs will provide additional insights during execution.

# Key Files
* Kafka Configuration: src/main/resources/config.properties

* Test Suite: src/test/java/org/johnny/app/AppIT.scala

# AI Log Analyzer & Root Cause Detection System

A production-grade observability platform designed to automatically analyze application logs, detect anomalies, identify likely root causes, and generate actionable remediation guidance.

## Project Structure

The project is organized into the following components:

- **`log-analyzer-service/`**: The core backend service built with Spring Boot.
  - **`src/main/java/`**: Contains the application logic for log ingestion, normalization, clustering, and anomaly detection.
  - **`src/main/resources/`**: Configuration files (`application.yml`) and prompts.
  - **`pom.xml`**: Maven build configuration for the analyzer service.
- **`log-producer-simulator/`**: A utility application to generate synthetic log traffic.
  - **`src/main/java/`**: Logic for generating random logs with varying error rates.
  - **`pom.xml`**: Maven build configuration for the simulator.
- **`docker/`**: Infrastructure configuration.
  - **`docker-compose.yml`**: Defines the services (Kafka, Elasticsearch, PostgreSQL, Zookeeper).
- **`verify_deployment.sh`**: A shell script to verify that all components are up and running correctly.

## Architecture

- **Ingestion**: Apache Kafka (Port 9093)
- **Analysis**: Spring Boot (Deterministic Normalization, Clustering, Anomaly Detection)
- **Intelligence**: Spring AI (Root Cause Analysis with LLM)
- **Persistence**: Elasticsearch (Logs), PostgreSQL (Incidents)

## Prerequisites

- **Java**: JDK 21
- **Docker** & **Docker Compose**
- **Maven**: 3.8+
- **OpenAI API Key**: Required for Root Cause Analysis (RCA) features.

## Setup & Run

### 1. Start Infrastructure

Start Kafka, Elasticsearch, and PostgreSQL using Docker Compose. 
*Note: Kafka is configured to run on port `9093` to avoid conflicts with common local services.*

```bash
docker compose -f docker/docker-compose.yml up -d
```

### 2. Build Services

Since this is a multi-module project without a parent POM, you need to build the services individually.

**Build Log Analyzer Service:**
```bash
cd log-analyzer-service
mvn clean install -DskipTests
cd ..
```

**Build Log Producer Simulator:**
```bash
cd log-producer-simulator
mvn clean install -DskipTests
cd ..
```

### 3. Run Log Analyzer Service

Run the main service. Make sure to export your OpenAI API key if you want to use RCA features.

```bash
export OPENAI_API_KEY=sk-your-key...
java -jar log-analyzer-service/target/log-analyzer-service-0.0.1-SNAPSHOT.jar
```

### 4. Run Log Producer Simulator

Start the simulator to generate synthetic traffic.

```bash
java -jar log-producer-simulator/target/log-producer-simulator-0.0.1-SNAPSHOT.jar
```

## How It Works

1. **Simulator** sends logs to `app-logs` Kafka topic.
2. **Analyzer** consumes logs:
   - Normalizes sensitive data (IPs, UUIDs).
   - Clusters errors by stack trace signature.
   - Detects anomalies (e.g., Error Bursts).
3. **Persister** saves logs to ES and Anomalies to Postgres.
4. **Alerter** logs `[ALERT]` to console when Incidents are created.
5. **AI Analysis** (Optional):
   - Trigger RCA: `POST http://localhost:8080/api/incidents/{id}/analyze`
   - Response includes Summary, Root Cause, and Fix Actions.

## Verification

Run the helper script to check deployment status:

```bash
./verify_deployment.sh
```

## Troubleshooting

### Port Conflicts
- **Kafka**: Configured on port **9093** (external) and **9092** (internal) to avoid conflicts with other local agents or services. Ensure `docker-compose.yml` maps `9093:9093`.

### Deserialization Errors
- If you see `RecordDeserializationException`, ensure the `application.yml` in `log-analyzer-service` has the default type configured:
  ```yaml
  spring.json.value.default.type: "com.company.loganalyzer.model.LogEvent"
  ```

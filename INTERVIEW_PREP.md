# AI Log Analyzer - Complete Interview Preparation Guide

This document covers everything from basic to advanced concepts that could be asked in an interview about this project.

---

## 1. Project Overview

### What is this project?
An **AI-powered Log Analyzer** that:
1. **Ingests** application logs via **Apache Kafka**
2. **Analyzes** logs using deterministic algorithms (normalization, clustering, anomaly detection)
3. **Persists** data to **Elasticsearch** (logs) and **PostgreSQL** (incidents)
4. **Alerts** when anomalies are detected
5. **Generates AI-powered Root Cause Analysis** using **Spring AI** and **OpenAI GPT**

### Tech Stack
| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Messaging | Apache Kafka |
| Search/Logs | Elasticsearch 8.x |
| Database | PostgreSQL 16 |
| AI/LLM | Spring AI + OpenAI |
| Build | Maven |
| Containers | Docker Compose |

---

## 2. Architecture Deep Dive

```
┌─────────────────────┐     ┌─────────────────────┐
│  Log Producer       │────▶│  Kafka (app-logs)   │
│  (Simulator)        │     │  Port: 9093         │
└─────────────────────┘     └──────────┬──────────┘
                                       │
                                       ▼
                           ┌─────────────────────┐
                           │  Log Analyzer       │
                           │  (Spring Boot)      │
                           │                     │
                           │  ┌───────────────┐  │
                           │  │ Normalizer    │  │
                           │  │ Clusterer     │  │
                           │  │ AnomalyDetect │  │
                           │  └───────────────┘  │
                           └──────────┬──────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
           ┌────────────┐    ┌────────────┐    ┌────────────┐
           │Elasticsearch│    │ PostgreSQL │    │   Alert    │
           │   (Logs)   │    │ (Incidents)│    │  Service   │
           └────────────┘    └────────────┘    └────────────┘
```

### Data Flow (Interview Favorite ⭐)
1. **Producer** generates log events and sends to Kafka topic `app-logs`
2. **Consumer** (LogIngestionService) receives events via `@KafkaListener`
3. **Pipeline** executes: Normalize → Cluster → Persist → Detect Anomaly → Create Incident → Alert
4. **API** exposes REST endpoints for querying logs and incidents

---

## 3. Important Code Files (Highlight ⭐)

### 📁 log-analyzer-service/src/main/java/com/company/loganalyzer/

| File | Purpose | Key Concepts |
|------|---------|--------------|
| **`ingestion/LogIngestionService.java`** | Core Kafka consumer, orchestrates entire pipeline | `@KafkaListener`, `@Transactional`, Dependency Injection |
| **`analysis/LogNormalizer.java`** | Masks sensitive data (IPs, UUIDs, numbers) | Regex patterns, data anonymization |
| **`analysis/ErrorClusterer.java`** | Groups similar errors by stack trace signature | SHA-256 hashing, content fingerprinting |
| **`analysis/AnomalyDetector.java`** | Detects error bursts using sliding window | ConcurrentHashMap, time-based windowing |
| **`ai/AiRootCauseService.java`** | LLM-powered root cause analysis | Spring AI, prompt engineering, structured output |
| **`model/LogEvent.java`** | Immutable DTO for Kafka messages | Java Records |
| **`model/LogDocument.java`** | Elasticsearch document mapping | Spring Data ES annotations |
| **`model/IncidentEntity.java`** | JPA entity for PostgreSQL | Jakarta Persistence API |
| **`controller/ApiController.java`** | REST API for logs/incidents | Spring MVC, Response filtering |
| **`alerting/ConsoleAlertService.java`** | Alert implementation | Interface-based design pattern |

### 📁 log-producer-simulator/

| File | Purpose |
|------|---------|
| **`LogGenerator.java`** | Generates synthetic log traffic with scheduled tasks |

---

## 4. Key Code Snippets & Interview Questions

### 4.1 Log Normalization (Data Masking)
**File**: `LogNormalizer.java`

```java
private static final Pattern UUID_PATTERN = Pattern
    .compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

public String normalize(String message) {
    String normalized = UUID_PATTERN.matcher(message).replaceAll("<UUID>");
    normalized = IP_PATTERN.matcher(normalized).replaceAll("<IP>");
    return normalized;
}
```

**Interview Questions:**
- *Why normalize logs?* → For clustering similar errors, GDPR/PII compliance, reducing noise
- *What regex concepts are used?* → Character classes, quantifiers, capture groups
- *How would you extend this?* → Add email, credit card patterns; use strategy pattern

---

### 4.2 Error Clustering (Hashing)
**File**: `ErrorClusterer.java`

```java
public String generateClusterId(String normalizedMessage, String stackTrace) {
    // Take first 3 lines of stack trace (captures the exception type and location)
    String[] lines = stackTrace.split("\n");
    StringBuilder sb = new StringBuilder(normalizedMessage);
    for (int i = 0; i < Math.min(lines.length, 3); i++) {
        sb.append(lines[i]);
    }
    
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash).substring(0, 16);
}
```

**Interview Questions:**
- *Why SHA-256?* → Deterministic, collision-resistant, fast
- *Why only first 3 lines?* → Captures exception type + location, ignores dynamic parts
- *How would you improve clustering?* → Machine learning (e.g., TF-IDF, embeddings)

---

### 4.3 Anomaly Detection (Sliding Window)
**File**: `AnomalyDetector.java`

```java
private final Map<String, Deque<Instant>> errorWindows = new ConcurrentHashMap<>();
private static final int ERROR_THRESHOLD = 5;
private static final int WINDOW_SECONDS = 60;

private boolean isErrorBurst(String serviceName) {
    Deque<Instant> times = errorWindows.computeIfAbsent(serviceName, k -> new ArrayDeque<>());
    Instant now = Instant.now();
    
    synchronized (times) {
        times.addLast(now);
        // Remove errors older than window
        while (!times.isEmpty() && times.peekFirst().isBefore(now.minusSeconds(WINDOW_SECONDS))) {
            times.removeFirst();
        }
        return times.size() > ERROR_THRESHOLD;
    }
}
```

**Interview Questions:**
- *Why ConcurrentHashMap?* → Thread-safe for multi-threaded Kafka consumers
- *Why synchronized block?* → Deque operations aren't atomic, need consistency
- *Limitations?* → In-memory (lost on restart), single-node only
- *How to improve?* → Use Redis with TTL, or Apache Flink for stream processing

---

### 4.4 Kafka Consumer
**File**: `LogIngestionService.java`

```java
@KafkaListener(topics = KafkaConfig.TOPIC_APP_LOGS, groupId = "log-analyzer-group")
@Transactional
public void consumeLogs(LogEvent logEvent) {
    // 1. Normalize
    String normalizedMessage = logNormalizer.normalize(logEvent.message());
    
    // 2. Cluster
    String clusterId = errorClusterer.generateClusterId(normalizedMessage, logEvent.stackTrace());
    
    // 3. Persist to Elasticsearch
    logRepository.save(logDoc);
    
    // 4. Detect Anomalies
    List<AnomalyType> anomalies = anomalyDetector.detectAnomalies(...);
    
    // 5. Create Incident if anomaly detected
    if (!anomalies.isEmpty()) {
        createOrUpdateIncident(serviceName, anomalies);
    }
}
```

**Interview Questions:**
- *What is @KafkaListener?* → Spring Kafka annotation for consuming messages
- *What is groupId?* → Consumer group for parallel processing and offset tracking
- *Why @Transactional?* → Atomic database operations; rollback on failure
- *How to handle failures?* → Use ErrorHandler, DLQ (Dead Letter Queue)

---

### 4.5 AI Root Cause Analysis (Spring AI)
**File**: `AiRootCauseService.java`

```java
public RootCauseAnalysis analyzeIncident(Long incidentId) {
    String systemPrompt = """
        You are a Senior Site Reliability Engineer.
        Analyze the following logs and incident details to determine the root cause.
        Output JSON matching this structure:
        {
            "summary": "...",
            "probableRootCause": "...",
            "recommendedActions": ["action1", "action2"],
            "confidenceScore": 0.95
        }
        """;
    
    return chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .entity(RootCauseAnalysis.class);  // Structured output!
}
```

**Interview Questions:**
- *What is Spring AI?* → Spring's abstraction over LLM providers (OpenAI, Ollama, etc.)
- *What is prompt engineering?* → Crafting effective prompts for LLMs (system + user messages)
- *What is `.entity()`?* → Structured output - LLM returns JSON mapped to Java object
- *How to handle LLM failures?* → Retry logic, fallback responses, rate limiting

---

### 4.6 Java Records (Modern Java)
**File**: `LogEvent.java`

```java
public record LogEvent(
    String serviceName,
    String level,
    String message,
    String stackTrace,
    Instant timestamp,
    Map<String, String> metadata) {
}
```

**Interview Questions:**
- *What are Java Records?* → Immutable data carriers with auto-generated equals/hashCode/toString
- *Why use records?* → Less boilerplate, immutability, perfect for DTOs
- *Limitations?* → Can't extend classes, fields are final

---

### 4.7 Spring Data Elasticsearch
**File**: `LogDocument.java`

```java
@Document(indexName = "logs")
public class LogDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String serviceName;
    
    @Field(type = FieldType.Text)
    private String message;
    
    @Field(type = FieldType.Date)
    private Instant timestamp;
}
```

**Interview Questions:**
- *Keyword vs Text?* → Keyword for exact match/aggregations, Text for full-text search
- *Why Elasticsearch for logs?* → Fast search, aggregations, time-series optimization
- *How to query?* → Repository methods or QueryBuilder

---

### 4.8 JPA Entity (PostgreSQL)
**File**: `IncidentEntity.java`

```java
@Entity
@Table(name = "incidents")
public class IncidentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private AnomalyType type;
    
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;
}
```

**Interview Questions:**
- *Why EnumType.STRING?* → Human-readable in DB, survives enum reordering
- *Why PostgreSQL for incidents?* → ACID transactions, relational queries, joins

---

## 5. Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Strategy** | AlertService interface | Swap alert implementations (Console, Slack, PagerDuty) |
| **Pipeline** | LogIngestionService | Sequential processing stages |
| **Repository** | Spring Data JPA/ES | Abstract data access |
| **Builder** | ChatClient.prompt() | Fluent API for LLM calls |
| **Dependency Injection** | Constructor injection | Loose coupling, testability |

---

## 6. Configuration & DevOps

### Docker Compose Services
```yaml
services:
  kafka:        # Port 9093 (external)
  zookeeper:    # Port 2181
  elasticsearch: # Port 9200
  postgres:     # Port 5432
```

### Application Configuration Key Points
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9093
    consumer:
      group-id: log-analyzer-group
      properties:
        spring.json.value.default.type: "com.company.loganalyzer.model.LogEvent"
```

**Interview Question:** *Why `spring.json.value.default.type`?*  
→ Producer doesn't send type headers, so consumer needs default type for deserialization

---

## 7. Common Interview Questions

### Architecture
1. *Why Kafka instead of REST?* → Async, decoupled, handles backpressure, replay capability
2. *Why both ES and PostgreSQL?* → ES for fast log search, PostgreSQL for transactional incident management
3. *How would you scale this?* → Kafka partitions, multiple consumer instances, ES cluster

### Spring Boot
1. *What is @Transactional?* → Declares transaction boundary, auto rollback on exceptions
2. *What is @KafkaListener?* → Annotation-driven Kafka consumer
3. *How does Spring AI work?* → Abstracts LLM APIs, provides ChatClient for interactions

### Java
1. *What are Records?* → Immutable data classes (Java 14+)
2. *Why ConcurrentHashMap?* → Thread-safe HashMap without full synchronization
3. *What is computeIfAbsent?* → Atomic put-if-not-present operation

### System Design
1. *How to handle log spikes?* → Kafka buffering, consumer scaling, circuit breaker
2. *How to ensure no log loss?* → Kafka ACKs, consumer commits, DLQ
3. *How to improve anomaly detection?* → ML models, more anomaly types, external alerting

---

## 8. File Structure Reference

```
log-analyzer-service/
├── docker/
│   └── docker-compose.yml          # Infrastructure config
├── log-analyzer-service/
│   ├── pom.xml                     # Maven dependencies
│   └── src/main/java/.../
│       ├── LogAnalyzerServiceApplication.java  # Main class
│       ├── ai/
│       │   └── AiRootCauseService.java        # ⭐ LLM integration
│       ├── alerting/
│       │   ├── AlertService.java              # Interface
│       │   └── ConsoleAlertService.java       # Implementation
│       ├── analysis/
│       │   ├── AnomalyDetector.java           # ⭐ Sliding window
│       │   ├── ErrorClusterer.java            # ⭐ SHA-256 hashing
│       │   └── LogNormalizer.java             # ⭐ Regex masking
│       ├── config/
│       │   └── KafkaConfig.java               # Topic constants
│       ├── controller/
│       │   └── ApiController.java             # ⭐ REST endpoints
│       ├── ingestion/
│       │   └── LogIngestionService.java       # ⭐ Kafka consumer
│       ├── model/
│       │   ├── IncidentEntity.java            # ⭐ JPA entity
│       │   ├── LogDocument.java               # ⭐ ES document
│       │   └── LogEvent.java                  # ⭐ Java Record
│       └── repository/
│           ├── IncidentRepository.java        # JPA repository
│           └── LogRepository.java             # ES repository
└── log-producer-simulator/
    └── src/main/java/.../
        └── LogGenerator.java                  # ⭐ Kafka producer
```

---

## 9. Quick Commands Reference

```bash
# Start infrastructure
docker compose -f docker/docker-compose.yml up -d

# Build services
cd log-analyzer-service && mvn clean install -DskipTests
cd log-producer-simulator && mvn clean install -DskipTests

# Run services
java -jar log-analyzer-service/target/log-analyzer-service-0.0.1-SNAPSHOT.jar
java -jar log-producer-simulator/target/log-producer-simulator-0.0.1-SNAPSHOT.jar

# API endpoints
GET http://localhost:8080/api/incidents
GET http://localhost:8080/api/logs
GET http://localhost:8080/api/logs/clusters
POST http://localhost:8080/api/incidents/{id}/analyze  # Trigger RCA
```

---

> **Tip for Interviews**: Focus on the **processing pipeline** in `LogIngestionService.java` - it demonstrates understanding of event-driven architecture, data transformation, and system integration.

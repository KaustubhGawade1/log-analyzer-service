# 🚀 AI Log Analyzer & Root Cause Detection System — Complete Walkthrough

> **Last verified**: February 23, 2026  
> **Status**: ✅ All services running and verified

---

## 📋 Table of Contents

1. [System Overview](#1-system-overview)
2. [Architecture](#2-architecture)
3. [Tech Stack](#3-tech-stack)
4. [Project Structure](#4-project-structure)
5. [Components Deep Dive](#5-components-deep-dive)
6. [How to Run](#6-how-to-run)
7. [Service Verification](#7-service-verification)
8. [Data Flow Walkthrough](#8-data-flow-walkthrough)
9. [API Reference](#9-api-reference)
10. [Dashboard & Visualizations](#10-dashboard--visualizations)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. System Overview

This is a **production-grade observability platform** that automatically:

- 📥 **Ingests** application logs from distributed microservices via Apache Kafka
- 🔍 **Normalizes** sensitive data (IPs, UUIDs) for privacy compliance
- 🔗 **Clusters** similar errors using SHA-256 stack trace fingerprinting
- 🚨 **Detects anomalies** (error bursts) using a sliding window algorithm
- 🤖 **Generates AI-powered Root Cause Analysis** using Spring AI + OpenAI
- 📊 **Visualizes** distributed traces using Zipkin + React Flow
- 💾 **Persists** logs to Elasticsearch and incidents to PostgreSQL

---

## 2. Architecture

### High-Level Architecture

```
┌─────────────────────┐     ┌─────────────────────┐
│  Log Producer       │────▶│  Kafka (app-logs)   │
│  (Simulator)        │     │  Port: 9093         │
└─────────────────────┘     └──────────┬──────────┘
                                       │
                           ┌───────────▼───────────┐
                           │  Log Analyzer Service  │
                           │  (Spring Boot :8080)   │
                           │                        │
                           │  ┌──────────────────┐  │
                           │  │ LogNormalizer     │  │
                           │  │ ErrorClusterer    │  │
                           │  │ AnomalyDetector   │  │
                           │  │ AiRootCauseService│  │
                           │  └──────────────────┘  │
                           └──────────┬─────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
           ┌────────────┐    ┌────────────┐    ┌────────────┐
           │Elasticsearch│    │ PostgreSQL │    │   Alert    │
           │  (Logs)     │    │ (Incidents)│    │  Service   │
           │  :9200      │    │  :5432     │    │ (Console)  │
           └────────────┘    └────────────┘    └────────────┘
```

### Microservices Demo Architecture (for Distributed Traces)

```
Client → API Gateway → User Service → Order Service → Inventory + Payment
         (8081)         (8082)         (8083)         (8084)    (8085)
                              ↓                          ↓
                           Zipkin (9411)              Kafka → Log Analyzer
```

---

## 3. Tech Stack

| Layer             | Technology                          | Port  |
|-------------------|-------------------------------------|-------|
| **Language**      | Java 21                             | —     |
| **Framework**     | Spring Boot 3.5                     | —     |
| **Messaging**     | Apache Kafka (Confluent 7.5)        | 9093  |
| **Log Storage**   | Elasticsearch 8.11                  | 9200  |
| **Incident DB**   | PostgreSQL 16                       | 5432  |
| **AI/LLM**        | Spring AI + OpenAI GPT-3.5-turbo   | —     |
| **Tracing**       | Zipkin + Micrometer Brave           | 9411  |
| **Frontend**      | React + Vite + React Flow           | 5173  |
| **Build**         | Maven 3.9+                          | —     |
| **Containers**    | Docker Compose                      | —     |
| **Coordinator**   | Zookeeper                           | 2181  |

---

## 4. Project Structure

```
log-analyzer-service/                   # Root project
│
├── README.md                           # Main README
├── INTERVIEW_PREP.md                   # Interview questions guide
├── API_FLOW_VISUALIZER_DOCS.md         # API Flow feature docs
├── WALKTHROUGH.md                      # This file
├── compose.yaml                        # Root compose (Spring Boot auto-config)
├── verify_deployment.sh                # Deployment verification script
│
├── docker/                             # Core infrastructure
│   └── docker-compose.yml              # Kafka, Zookeeper, ES, PostgreSQL
│
├── log-analyzer-service/               # 🧠 Core Analysis Engine
│   ├── pom.xml
│   └── src/main/java/com/company/loganalyzer/
│       ├── LogAnalyzerServiceApplication.java
│       ├── ai/
│       │   ├── AiRootCauseService.java         # LLM Root Cause Analysis
│       │   └── AiFlowExplanationService.java   # LLM Flow Explanations
│       ├── alerting/
│       │   ├── AlertService.java               # Interface (Strategy Pattern)
│       │   └── ConsoleAlertService.java         # Implementation
│       ├── analysis/
│       │   ├── AnomalyDetector.java            # Sliding window anomaly detection
│       │   ├── ErrorClusterer.java             # SHA-256 error clustering
│       │   └── LogNormalizer.java              # Regex PII masking
│       ├── config/
│       │   ├── CorsConfig.java                 # CORS configuration
│       │   ├── KafkaConfig.java                # Topic constants
│       │   └── ZipkinConfig.java               # Zipkin REST client config
│       ├── controller/
│       │   ├── ApiController.java              # Log/Incident REST API
│       │   └── FlowController.java             # Flow visualization API
│       ├── flow/                               # API Flow Visualizer
│       │   ├── entity/                         # JPA entities
│       │   ├── model/                          # Domain models
│       │   ├── repository/                     # Data access
│       │   └── service/                        # Business logic
│       ├── ingestion/
│       │   └── LogIngestionService.java        # Kafka consumer & pipeline
│       ├── model/
│       │   ├── LogEvent.java                   # Java Record (Kafka DTO)
│       │   ├── LogDocument.java                # ES document
│       │   └── IncidentEntity.java             # JPA entity
│       └── repository/
│           ├── LogRepository.java              # Elasticsearch
│           └── IncidentRepository.java         # PostgreSQL
│
├── log-producer-simulator/             # 📤 Synthetic Log Generator
│   ├── pom.xml
│   └── src/main/java/
│       └── LogGenerator.java                   # Scheduled Kafka producer
│
├── microservices-demo/                 # 🌐 5-Service Demo Environment
│   ├── README.md
│   ├── docker-compose.yml              # Full stack with Zipkin + Kafka
│   ├── demo.sh                         # Traffic generation script
│   ├── pom.xml                         # Parent POM
│   ├── api-gateway/         (8081)     # Entry point, routing
│   ├── user-service/        (8082)     # User management
│   ├── order-service/       (8083)     # Order orchestration
│   ├── inventory-service/   (8084)     # Stock management
│   └── payment-service/     (8085)     # Payment processing
│
└── dashboard/                          # 📊 React Frontend
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx                     # Router with lazy loading
        ├── components/
        │   ├── Sidebar.jsx             # Navigation
        │   └── graph/                  # React Flow components
        │       ├── FlowGraph.jsx       # Flow graph visualization
        │       ├── ServiceNode.jsx     # Custom service node
        │       └── FlowEdge.jsx        # Custom edge with metrics
        ├── pages/
        │   ├── Dashboard.jsx           # Main dashboard
        │   ├── ApiFlows.jsx            # API Flow Explorer
        │   ├── Incidents.jsx           # Incident management
        │   ├── LogExplorer.jsx         # Log search
        │   └── ErrorClusters.jsx       # Error cluster view
        └── services/
            └── api.js                  # Backend API client
```

---

## 5. Components Deep Dive

### 5.1 Log Analyzer Service (Core Engine)

The heart of the system. Receives logs from Kafka, processes them through a pipeline, and stores results.

**Processing Pipeline (in `LogIngestionService.java`):**
1. **Normalize** → Mask IPs, UUIDs, numbers via regex
2. **Cluster** → Generate SHA-256 fingerprint from normalized message + stack trace
3. **Persist** → Save to Elasticsearch
4. **Detect Anomalies** → Sliding window error burst detection
5. **Create Incident** → Store in PostgreSQL if anomaly detected
6. **Alert** → Log `[ALERT]` to console

**Key Algorithms:**
- **Normalization**: Regex-based PII masking (`\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}` → `<IP>`)
- **Clustering**: SHA-256 hash of normalized message + first 3 lines of stack trace
- **Anomaly Detection**: `ConcurrentHashMap<String, Deque<Instant>>` with 60-second sliding window, threshold of 5 errors

### 5.2 Log Producer Simulator

Generates realistic synthetic logs with:
- Random service names
- Varying log levels (INFO, WARN, ERROR)
- Stack traces for errors
- Configurable error rates
- Scheduled via `@Scheduled`

### 5.3 Microservices Demo (5 Services)

A complete e-commerce microservices setup that:
- Generates **distributed traces** for the API Flow Visualizer
- Sends **logs to Kafka** for the Log Analyzer
- Supports **chaos engineering** (random latency, error injection)

**Order Flow:**
```
API Gateway (8081)
  └─ GET /api/users/{id} → User Service (8082) → Validates user
  └─ POST /orders → Order Service (8083)
       └─ POST /inventory/reserve → Inventory Service (8084)
       └─ POST /payments/process → Payment Service (8085)
```

Each service:
- Reports traces to **Zipkin** via Micrometer Brave
- Forwards logs to **Kafka** (`app-logs` topic) via `KafkaLogForwarder`
- Supports chaos mode via environment variables

### 5.4 Dashboard (React Frontend)

5 pages with a dark-themed, glassmorphism UI:

| Page | Route | Description |
|------|-------|-------------|
| Dashboard | `/` | Incident summary, data pipelines, log timeline |
| Incidents | `/incidents` | View/manage detected incidents |
| Log Explorer | `/logs` | Full-text search through ingested logs |
| Error Clusters | `/clusters` | View grouped errors by fingerprint |
| API Flows | `/flows` | React Flow visualization of distributed traces |

### 5.5 API Flow Visualizer

Connects to **Zipkin** to fetch distributed traces, converts them to interactive node graphs:
- **FlowGraphBuilder** → Converts Zipkin spans to nodes/edges
- **BottleneckDetector** → Identifies high latency, error rates, cascading failures
- **AI Explanation** → Uses OpenAI to explain complex call graphs

---

## 6. How to Run

### Prerequisites
- Java 21 (JDK)
- Docker & Docker Compose
- Maven 3.9+
- Node.js 18+ & npm
- (Optional) OpenAI API Key for AI features

### Step 1: Start Infrastructure (Docker)

```bash
cd /home/kaustubhgawade/Downloads/log-analyzer-service

# Start Kafka, Zookeeper, Elasticsearch, PostgreSQL
docker compose -f docker/docker-compose.yml up -d

# Start Zipkin (for distributed tracing)
docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin
```

**Verify infrastructure:**
```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Expected containers:
| Container        | Port |
|------------------|------|
| kafka            | 9093 |
| zookeeper        | 2181 |
| elasticsearch    | 9200 |
| postgres         | 5432 |
| demo-zipkin      | 9411 |

### Step 2: Build All Services

```bash
# Build the core log analyzer
cd log-analyzer-service && mvn clean install -DskipTests && cd ..

# Build the log producer simulator
cd log-producer-simulator && mvn clean install -DskipTests && cd ..

# Build all 5 microservices
cd microservices-demo && mvn clean install -DskipTests && cd ..
```

### Step 3: Start the Log Analyzer Service (Core Backend)

```bash
# Optional: Set OpenAI key for AI features
export OPENAI_API_KEY=sk-your-key-here

# Start on port 8080
java -jar log-analyzer-service/target/log-analyzer-service-0.0.1-SNAPSHOT.jar &
```

### Step 4: Start the 5 Microservices

```bash
cd microservices-demo

# Start each service (each in its own terminal or background)
mvn spring-boot:run -pl api-gateway &
mvn spring-boot:run -pl user-service &
mvn spring-boot:run -pl order-service &
mvn spring-boot:run -pl inventory-service &
mvn spring-boot:run -pl payment-service &
```

### Step 5: Start the Dashboard (Frontend)

```bash
cd dashboard
npm install   # First time only
npm run dev
```

### Step 6: Generate Demo Traffic

```bash
cd microservices-demo
chmod +x demo.sh
./demo.sh http://localhost:8081 10 2
```

Or manually test:
```bash
# Health check
curl http://localhost:8081/api/health

# Full order flow (creates traces through all 5 services)
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","productId":"product-1","quantity":2,"amount":59.98}'
```

### Step 7: Start Log Producer Simulator (Optional)

```bash
java -jar log-producer-simulator/target/log-producer-simulator-0.0.1-SNAPSHOT.jar &
```

---

## 7. Service Verification

### All Service URLs

| Service                  | URL                              | Status |
|--------------------------|----------------------------------|--------|
| **Log Analyzer API**     | http://localhost:8080             | ✅     |
| **API Gateway**          | http://localhost:8081/api/health  | ✅     |
| **User Service**         | http://localhost:8082/users/user-1| ✅     |
| **Order Service**        | http://localhost:8083             | ✅     |
| **Inventory Service**    | http://localhost:8084/inventory/product-1 | ✅ |
| **Payment Service**      | http://localhost:8085             | ✅     |
| **Dashboard**            | http://localhost:5173             | ✅     |
| **API Flow Visualizer**  | http://localhost:5173/flows       | ✅     |
| **Zipkin UI**            | http://localhost:9411             | ✅     |
| **Elasticsearch**        | http://localhost:9200             | ✅     |

### Quick Health Check Script

```bash
for port in 8080 8081 8082 8083 8084 8085; do
  echo -n "Port $port: "
  curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/ 2>/dev/null
  echo ""
done
```

---

## 8. Data Flow Walkthrough

### Flow 1: Log Ingestion & Analysis

```
1. Log Producer Simulator generates LogEvent:
   {serviceName: "order-service", level: "ERROR", message: "...", stackTrace: "..."}

2. LogEvent → Kafka topic "app-logs" (port 9093)

3. LogIngestionService (@KafkaListener) consumes:
   a. LogNormalizer masks IPs/UUIDs → "Connection timeout from <IP> for user <UUID>"
   b. ErrorClusterer generates SHA-256 fingerprint → "a1b2c3d4e5f67890"
   c. LogDocument saved to Elasticsearch (searchable)
   d. AnomalyDetector checks sliding window → ERROR_BURST if >5 errors/60s
   e. IncidentEntity created in PostgreSQL
   f. ConsoleAlertService logs [ALERT]

4. Dashboard queries:
   - GET /api/incidents → PostgreSQL incidents
   - GET /api/logs → Elasticsearch logs
   - GET /api/logs/clusters → Grouped by cluster ID
```

### Flow 2: Distributed Trace Visualization

```
1. Client calls POST /api/orders on API Gateway (8081)

2. API Gateway:
   └─ Creates Zipkin span (root)
   └─ Calls User Service (8082) → child span
   └─ Calls Order Service (8083) → child span
       └─ Order Service calls Inventory (8084) → child span
       └─ Order Service calls Payment (8085) → child span

3. All spans sent to Zipkin (9411)

4. KafkaLogForwarder in each service sends logs to Kafka

5. Dashboard (API Flow Explorer):
   - Fetches traces from Zipkin via backend
   - FlowGraphBuilder converts spans to graph
   - BottleneckDetector flags slow/failing services
   - React Flow renders interactive visualization
```

### Flow 3: AI Root Cause Analysis

```
1. Incident detected (ERROR_BURST in payment-service)

2. User triggers: POST /api/incidents/{id}/analyze

3. AiRootCauseService:
   a. Fetches incident + related logs from ES
   b. Constructs prompt (system + user context)
   c. Calls OpenAI GPT-3.5-turbo via Spring AI
   d. Receives structured JSON: {summary, rootCause, actions, confidence}

4. Response returned to dashboard for display
```

---

## 9. API Reference

### Log Analyzer Service (Port 8080)

| Method | Endpoint                       | Description                    |
|--------|--------------------------------|--------------------------------|
| GET    | `/api/incidents`               | List all incidents             |
| GET    | `/api/incidents/{id}`          | Get incident details           |
| POST   | `/api/incidents/{id}/analyze`  | Trigger AI Root Cause Analysis |
| GET    | `/api/logs`                    | List recent logs               |
| GET    | `/api/logs/clusters`           | List error clusters            |
| GET    | `/api/logs/timeline`           | Log activity timeline          |

### Flow Endpoints (Port 8080)

| Method | Endpoint                          | Description                 |
|--------|-----------------------------------|-----------------------------|
| GET    | `/api/flows/services`             | List services from Zipkin   |
| GET    | `/api/flows/dependencies`         | Service dependency graph    |
| GET    | `/api/flows/traces`               | List recent traces          |
| GET    | `/api/flows/{traceId}`            | Get flow graph for a trace  |
| GET    | `/api/flows/bottlenecks`          | Flows with detected issues  |
| POST   | `/api/flows/{traceId}/explain`    | AI explanation for flow     |
| GET    | `/api/flows/stats`                | Flow statistics             |
| GET    | `/api/flows/topology`             | Complete topology           |

### Microservices Demo

| Service           | Port | Key Endpoints                          |
|-------------------|------|----------------------------------------|
| **API Gateway**   | 8081 | `GET /api/health`, `POST /api/orders`  |
| **User Service**  | 8082 | `GET /users/{id}`, `POST /users/{id}/validate` |
| **Order Service** | 8083 | `POST /orders`, `GET /orders/{id}`     |
| **Inventory**     | 8084 | `GET /inventory/{id}`, `POST /inventory/reserve` |
| **Payment**       | 8085 | `POST /payments/process`, `GET /payments/{id}/status` |

---

## 10. Dashboard & Visualizations

### Main Dashboard (`/`)
- **Incident Summary**: Total, Open, Resolved, Error Bursts
- **Data Pipelines**: Kafka (log ingestion status), Zipkin (trace pipeline status)
- **Log Activity Timeline**: Real-time chart of log volume (last 60 mins)

### Incidents (`/incidents`)
- List all detected anomaly incidents
- View details per incident
- Trigger AI analysis

### Log Explorer (`/logs`)
- Full-text search across all ingested logs
- Filter by service, level, time range

### Error Clusters (`/clusters`)
- Errors grouped by SHA-256 fingerprint
- Shows count, first/last seen

### API Flow Explorer (`/flows`)
- Interactive service graph powered by React Flow
- Lists recent traces with latency metrics
- Click a trace to see the full distributed call graph
- AI explanation generator for complex flows

### Zipkin UI (`http://localhost:9411`)
- Native Zipkin interface for exploring raw spans
- Service dependency graph
- Latency distributions

---

## 11. Troubleshooting

### Kafka Not Starting
```bash
# Check Kafka logs
docker logs kafka

# Restart infrastructure
docker compose -f docker/docker-compose.yml down
docker compose -f docker/docker-compose.yml up -d
```

### Port Conflicts
| Service | Default Port | Fix |
|---------|-------------|-----|
| Kafka   | 9093        | Change in `docker-compose.yml` |
| ES      | 9200        | Change in `docker-compose.yml` |
| Postgres| 5432        | Change in `docker-compose.yml` |
| Backend | 8080        | Change `server.port` in `application.yml` |

### Deserialization Errors
If you see `RecordDeserializationException`, ensure `application.yml` has:
```yaml
spring.json.value.default.type: "com.company.loganalyzer.model.LogEvent"
```

### CORS Errors
Frontend ports (5173-5175) are whitelisted in `CorsConfig.java`.

### Spring Boot Docker Compose Auto-Config
If backend fails with "No Docker Compose file found", ensure:
```yaml
spring:
  docker:
    compose:
      enabled: false
```

### Checking Process Status
```bash
# Find Java processes
ps aux | grep java | grep -v grep

# Kill a specific service
kill $(lsof -t -i:8081)
```

---

## Design Patterns Used

| Pattern                | Where Used                    | Purpose                        |
|------------------------|-------------------------------|--------------------------------|
| **Strategy**           | `AlertService` interface      | Swap alert implementations     |
| **Pipeline**           | `LogIngestionService`         | Sequential processing stages   |
| **Repository**         | Spring Data JPA/ES            | Abstract data access           |
| **Builder**            | `ChatClient.prompt()`         | Fluent API for LLM calls       |
| **DI (Constructor)**   | All services                  | Loose coupling, testability    |
| **Observer**           | `@KafkaListener`              | Event-driven log consumption   |
| **Fingerprinting**     | `ErrorClusterer`              | Content-based deduplication    |

---

*This walkthrough was generated and verified with all services running on February 23, 2026.*

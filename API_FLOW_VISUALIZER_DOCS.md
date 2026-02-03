# API Flow Visualizer - Implementation Documentation

## Overview

This document details the complete implementation of the **API Flow Visualizer** feature added to the AI Log Analyzer platform. The feature enables visualization of distributed traces from Zipkin, showing service-to-service API call flows with latency metrics, error rates, and AI-powered insights.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Backend Implementation](#backend-implementation)
3. [Frontend Implementation](#frontend-implementation)
4. [Configuration Changes](#configuration-changes)
5. [Bug Fixes](#bug-fixes)
6. [How to Run](#how-to-run)
7. [API Reference](#api-reference)

---

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  React Frontend │───▶│  Spring Boot    │───▶│     Zipkin      │
│  (React Flow)   │    │  Backend API    │    │  Trace Server   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │    OpenAI       │
                       │  (Explanation)  │
                       └─────────────────┘
```

### Key Components
- **Zipkin Integration**: Fetches distributed traces
- **Flow Graph Builder**: Converts spans to graph structure
- **Bottleneck Detector**: Identifies performance issues
- **AI Explanation Service**: Generates natural language insights
- **React Flow UI**: Interactive graph visualization

---

## Backend Implementation

### New Packages Created

#### `flow/model/` - Domain Models

| File | Description |
|------|-------------|
| `ApiFlowGraph.java` | Complete flow graph with nodes, edges, and metadata |
| `ServiceNode.java` | Represents a service/endpoint in the flow |
| `FlowEdge.java` | Connection between services with metrics |
| `EdgeMetrics.java` | Latency, error rate, request count data |
| `TraceSpan.java` | Zipkin span data mapping |
| `DependencyGraph.java` | Service dependency topology |

#### `flow/entity/` - JPA Entities

| File | Description |
|------|-------------|
| `FlowSnapshotEntity.java` | Persists flow graph snapshots |
| `ServiceDependencyEntity.java` | Stores service dependencies |

#### `flow/repository/` - Data Access

| File | Description |
|------|-------------|
| `FlowSnapshotRepository.java` | CRUD for flow snapshots |
| `ServiceDependencyRepository.java` | CRUD for dependencies |

#### `flow/service/` - Business Logic

| File | Description |
|------|-------------|
| `ZipkinTraceClient.java` | REST client for Zipkin API |
| `FlowGraphBuilder.java` | Converts traces to graphs |
| `BottleneckDetector.java` | Detects latency/error issues |
| `FlowService.java` | Main orchestration service |

#### `ai/` - AI Integration

| File | Description |
|------|-------------|
| `AiFlowExplanationService.java` | Spring AI + OpenAI integration for flow explanations |

#### `controller/` - REST API

| File | Description |
|------|-------------|
| `FlowController.java` | API endpoints for flow visualization |

### Key Code: FlowGraphBuilder.java

```java
@Service
public class FlowGraphBuilder {
    
    public ApiFlowGraph buildFromTrace(List<TraceSpan> spans) {
        // 1. Build span map for parent-child relationships
        Map<String, TraceSpan> spanMap = spans.stream()
            .collect(Collectors.toMap(TraceSpan::spanId, s -> s));
        
        // 2. Create service nodes from unique services
        List<ServiceNode> nodes = createServiceNodes(spans);
        
        // 3. Create edges from parent-child relationships
        List<FlowEdge> edges = createFlowEdges(spans, spanMap);
        
        // 4. Calculate graph-level metrics
        return new ApiFlowGraph(traceId, nodes, edges, calculateMetrics(spans));
    }
}
```

### Key Code: BottleneckDetector.java

```java
@Service
public class BottleneckDetector {
    
    public List<Bottleneck> detectBottlenecks(ApiFlowGraph graph) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        
        // Check for high latency
        graph.getNodes().forEach(node -> {
            if (node.getAvgLatency() > LATENCY_THRESHOLD_MS) {
                bottlenecks.add(new Bottleneck(
                    BottleneckType.HIGH_LATENCY,
                    node.getId(),
                    calculateSeverity(node.getAvgLatency())
                ));
            }
        });
        
        // Check for high error rates
        // Check for high fan-out
        // Check for cascading failures
        
        return bottlenecks;
    }
}
```

---

## Frontend Implementation

### New Components Created

#### `components/graph/` - Graph Visualization

| File | Description |
|------|-------------|
| `FlowGraph.jsx` | Main React Flow component with hierarchical layout |
| `FlowGraph.css` | Styling for graph container |
| `ServiceNode.jsx` | Custom node with health indicators |
| `FlowEdge.jsx` | Custom edge with latency/error labels |

#### `pages/` - New Pages

| File | Description |
|------|-------------|
| `ApiFlows.jsx` | API Flow Explorer page |
| `ApiFlows.css` | Styling for the page layout |

### Key Code: FlowGraph.jsx

```jsx
// Simple hierarchical layout (replaces dagre to avoid ESM issues)
const getLayoutedElements = (nodes, edges) => {
    // Build adjacency map
    const children = new Map();
    const parents = new Map();
    
    // BFS to assign levels
    const levels = new Map();
    const queue = roots.map(r => ({ id: r.id, level: 0 }));
    
    while (queue.length > 0) {
        const { id, level } = queue.shift();
        levels.set(id, level);
        // Process children...
    }
    
    // Position nodes by level
    return layoutedNodes;
};

export default function FlowGraph({ graphData, onNodeClick }) {
    return (
        <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={{ service: ServiceNode }}
            edgeTypes={{ flow: FlowEdge }}
            fitView
        >
            <Background />
            <Controls />
            <MiniMap />
        </ReactFlow>
    );
}
```

### Navigation Updates

**App.jsx** - Added lazy loading for ApiFlows:
```jsx
import { Suspense, lazy } from 'react';

const ApiFlows = lazy(() => import('./pages/ApiFlows'));

<Route path="/flows" element={
    <Suspense fallback={<div>Loading...</div>}>
        <ApiFlows />
    </Suspense>
} />
```

**Sidebar.jsx** - Added navigation link:
```jsx
<NavLink to="/flows">
    <span>API Flows</span>
</NavLink>
```

### API Service Updates

**api.js** - New functions:
```javascript
export async function fetchFlowServices() { ... }
export async function fetchDependencies(lookbackMs) { ... }
export async function fetchTraces(options) { ... }
export async function fetchFlow(traceId) { ... }
export async function fetchBottleneckFlows(limit, lookbackMs) { ... }
export async function explainFlow(traceId) { ... }
export async function fetchFlowStats(lookbackMs) { ... }
export async function fetchTopology(lookbackMs) { ... }
```

---

## Configuration Changes

### application.yml

Added Zipkin configuration:
```yaml
spring:
  docker:
    compose:
      enabled: false  # Disable auto Docker Compose

zipkin:
  base-url: ${ZIPKIN_BASE_URL:http://localhost:9411}
  connect-timeout-ms: 5000
  read-timeout-ms: 10000
```

### CorsConfig.java

Added frontend development ports:
```java
config.setAllowedOrigins(Arrays.asList(
    "http://localhost:5173",
    "http://localhost:5174",
    "http://localhost:5175",
    "http://localhost:5180",
    "http://localhost:3000"
));
```

### vite.config.js

Optimized dependency handling:
```javascript
export default defineConfig({
  plugins: [react()],
  optimizeDeps: {
    include: ['reactflow'],
  },
})
```

---

## Bug Fixes

### 1. Dagre ESM Compatibility Issue

**Problem**: `@dagrejs/dagre` caused runtime error:
```
TypeError: Dynamic require of "@dagrejs/graphlib" is not supported
```

**Solution**: 
- Removed `@dagrejs/dagre` dependency
- Implemented custom hierarchical layout algorithm in `FlowGraph.jsx`
- Used React.lazy() for ApiFlows to isolate potential component errors

### 2. Spring Boot Docker Compose Auto-Configuration

**Problem**: Backend failed to start with:
```
No Docker Compose file found in directory
```

**Solution**: Added to `application.yml`:
```yaml
spring:
  docker:
    compose:
      enabled: false
```

### 3. CORS Blocking Frontend Requests

**Problem**: Frontend couldn't reach backend API due to CORS when using different development ports.

**Solution**: Updated `CorsConfig.java` to include ports 5173-5175.

### 4. Missing API Function Aliases

**Problem**: Build failed due to missing exports:
```
"fetchTimeline" is not exported by "src/services/api.js"
```

**Solution**: Added backward compatibility aliases:
```javascript
export const fetchTimeline = fetchLogTimeline;
export const fetchClusters = fetchLogClusters;
```

---

## How to Run

### Prerequisites
- Docker & Docker Compose
- Java 21
- Node.js 18+
- Maven 3.9+

### 1. Start Infrastructure
```bash
cd docker
docker compose up -d
```

### 2. Start Zipkin
```bash
docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin
```

### 3. Start Backend
```bash
cd log-analyzer-service
mvn spring-boot:run
```

### 4. Start Frontend
```bash
cd dashboard
npm install
npm run dev
```

### 5. Access the Application
- **Dashboard**: http://localhost:5173
- **API Flows**: http://localhost:5173/flows
- **Zipkin UI**: http://localhost:9411

---

## API Reference

### Flow Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/flows/services` | List all services from Zipkin |
| GET | `/api/flows/dependencies` | Get service dependency graph |
| GET | `/api/flows/traces` | List recent traces |
| GET | `/api/flows/{traceId}` | Get flow graph for a trace |
| GET | `/api/flows/bottlenecks` | Get flows with detected issues |
| POST | `/api/flows/{traceId}/explain` | Get AI explanation for flow |
| GET | `/api/flows/stats` | Get flow statistics |
| GET | `/api/flows/topology` | Get complete topology |

### Query Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `lookbackMs` | Time window in milliseconds | 3600000 (1 hour) |
| `limit` | Maximum number of results | 50 |
| `serviceName` | Filter by service name | - |

### Response Example

```json
{
  "traceId": "abc123",
  "nodes": [
    {
      "id": "api-gateway",
      "serviceName": "api-gateway",
      "health": "HEALTHY",
      "avgLatency": 45,
      "errorRate": 0.01
    }
  ],
  "edges": [
    {
      "id": "e1",
      "sourceNodeId": "api-gateway",
      "targetNodeId": "user-service",
      "metrics": {
        "avgLatency": 23,
        "errorRate": 0.0,
        "requestCount": 150
      }
    }
  ]
}
```

---

## Files Changed Summary

### Backend (17 new files)
```
src/main/java/com/company/loganalyzer/
├── ai/
│   └── AiFlowExplanationService.java
├── config/
│   ├── CorsConfig.java (modified)
│   └── ZipkinConfig.java
├── controller/
│   └── FlowController.java
└── flow/
    ├── entity/
    │   ├── FlowSnapshotEntity.java
    │   └── ServiceDependencyEntity.java
    ├── model/
    │   ├── ApiFlowGraph.java
    │   ├── DependencyGraph.java
    │   ├── EdgeMetrics.java
    │   ├── FlowEdge.java
    │   ├── ServiceNode.java
    │   └── TraceSpan.java
    ├── repository/
    │   ├── FlowSnapshotRepository.java
    │   └── ServiceDependencyRepository.java
    └── service/
        ├── BottleneckDetector.java
        ├── FlowGraphBuilder.java
        ├── FlowService.java
        └── ZipkinTraceClient.java
```

### Frontend (8 new/modified files)
```
dashboard/src/
├── components/
│   ├── Sidebar.jsx (modified)
│   └── graph/
│       ├── FlowEdge.jsx
│       ├── FlowGraph.css
│       ├── FlowGraph.jsx
│       └── ServiceNode.jsx
├── pages/
│   ├── ApiFlows.css
│   └── ApiFlows.jsx
├── services/
│   └── api.js (modified)
└── App.jsx (modified)
```

### Configuration (3 files)
```
├── application.yml (modified)
├── vite.config.js (modified)
└── package.json (dependencies added)
```

---

## Dependencies Added

### Backend (pom.xml)
- Already had Spring AI, Spring Data JPA, Kafka, Elasticsearch

### Frontend (package.json)
```json
{
  "dependencies": {
    "reactflow": "^11.x"
  }
}
```

Note: `@dagrejs/dagre` was initially added but later removed due to ESM compatibility issues.

---

## Current Status

| Component | Status |
|-----------|--------|
| Backend API | ✅ Running on :8080 |
| Frontend | ✅ Running on :5175 |
| Zipkin | ✅ Running on :9411 |
| Kafka | ✅ Running on :9093 |
| PostgreSQL | ✅ Running on :5432 |
| Elasticsearch | ✅ Running on :9200 |

---

*Document created: February 3, 2026*

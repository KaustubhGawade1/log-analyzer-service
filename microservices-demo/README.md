# Microservices Demo Environment

A mini microservices demo that generates distributed traces for the **API Flow Visualizer**.

## Architecture

```
Client → API Gateway → User Service → Order Service → Inventory + Payment
         (8081)         (8082)         (8083)         (8084)    (8085)
                              ↓
                           Zipkin (9411)
```

## Quick Start

### Option 1: Run Locally with Maven

```bash
# Terminal 1 - Start Zipkin (if not running)
docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin

# Terminal 2-6 - Start each service
cd api-gateway && mvn spring-boot:run &
cd user-service && mvn spring-boot:run &
cd order-service && mvn spring-boot:run &
cd inventory-service && mvn spring-boot:run &
cd payment-service && mvn spring-boot:run &
```

### Option 2: Run with Docker Compose

```bash
docker-compose up --build
```

## Generate Demo Traffic

```bash
chmod +x demo.sh
./demo.sh http://localhost:8081 10 2
```

This creates 10 orders with 2-second delays, generating traces through all services.

## API Endpoints

### API Gateway (8081)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/users/{id}` | Get user info |
| POST | `/api/orders` | Create order (full flow) |
| GET | `/api/orders/{id}` | Get order status |

### User Service (8082)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users/{id}` | Get user details |
| POST | `/users/{id}/validate` | Validate user |

### Order Service (8083)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/orders` | Create order |
| GET | `/orders/{id}` | Get order details |

### Inventory Service (8084)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/inventory/{productId}` | Check stock |
| POST | `/inventory/reserve` | Reserve items |

### Payment Service (8085)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/payments/process` | Process payment |
| GET | `/payments/{id}/status` | Get payment status |

## Chaos Engineering

Enable failures to test the API Flow Visualizer's bottleneck detection:

```bash
# Enable random latency (100-2000ms)
CHAOS_LATENCY_ENABLED=true docker-compose up -d

# Enable random errors (30% failure rate)
CHAOS_ERROR_ENABLED=true CHAOS_ERROR_RATE=0.3 docker-compose up -d

# Both combined
CHAOS_LATENCY_ENABLED=true CHAOS_ERROR_ENABLED=true docker-compose up -d
```

## Viewing Traces

1. **Zipkin UI**: http://localhost:9411
2. **API Flow Visualizer**: http://localhost:5175/flows

## Pre-populated Test Data

### Users
| ID | Name | Status |
|----|------|--------|
| user-1 | John Doe | ACTIVE |
| user-2 | Jane Smith | ACTIVE |
| user-3 | Bob Wilson | INACTIVE |

### Products
| ID | Stock |
|----|-------|
| product-1 | 100 |
| product-2 | 50 |
| product-3 | 200 |
| product-4 | 0 (out of stock) |

## How This Proves API Flow Visualizer Works

| Scenario | Expected Visualization |
|----------|------------------------|
| Normal order flow | 5-node graph with green edges |
| Enable latency | Orange/red edges on slow services |
| Enable errors | Red failing nodes, error rate shown |
| Multiple orders | Fan-out pattern visible (Order → Inventory + Payment) |

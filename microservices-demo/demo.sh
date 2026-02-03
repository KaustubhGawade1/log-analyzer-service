#!/bin/bash
# Demo Script for Microservices API Flow Visualizer
# This script generates traffic to create traces for visualization

BASE_URL=${1:-http://localhost:8081}
ITERATIONS=${2:-10}
DELAY=${3:-2}

echo "=============================================="
echo "Microservices Demo - API Flow Visualizer Demo"
echo "=============================================="
echo "Target: $BASE_URL"
echo "Iterations: $ITERATIONS"
echo "Delay: ${DELAY}s between requests"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to make request and show result
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "${YELLOW}► ${description}${NC}"
    echo "  $method $endpoint"
    
    if [ "$method" == "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "  ${GREEN}✓ HTTP $http_code${NC}"
    else
        echo -e "  ${RED}✗ HTTP $http_code${NC}"
    fi
    echo "  Response: $(echo $body | head -c 100)..."
    echo ""
}

echo "=============================================="
echo "Phase 1: Health Checks"
echo "=============================================="
make_request "GET" "/api/health" "" "Checking API Gateway health"

echo "=============================================="
echo "Phase 2: User Operations"
echo "=============================================="
make_request "GET" "/api/users/user-1" "" "Fetching user-1 (John Doe)"
make_request "GET" "/api/users/user-2" "" "Fetching user-2 (Jane Smith)"

echo "=============================================="
echo "Phase 3: Create Orders (Full Flow)"
echo "=============================================="
echo "This creates traces through all 5 services:"
echo "Gateway → User → Order → Inventory + Payment"
echo ""

for i in $(seq 1 $ITERATIONS); do
    echo "--- Order $i of $ITERATIONS ---"
    
    # Alternate between users
    if [ $((i % 2)) -eq 0 ]; then
        user_id="user-1"
    else
        user_id="user-2"
    fi
    
    # Randomize product and amount
    product_id="product-$((i % 3 + 1))"
    quantity=$((i % 5 + 1))
    amount=$(echo "scale=2; $quantity * 29.99" | bc)
    
    order_data="{\"userId\":\"$user_id\",\"productId\":\"$product_id\",\"quantity\":$quantity,\"amount\":$amount}"
    
    make_request "POST" "/api/orders" "$order_data" "Creating order for $user_id"
    
    if [ $i -lt $ITERATIONS ]; then
        sleep $DELAY
    fi
done

echo "=============================================="
echo "Demo Complete!"
echo "=============================================="
echo ""
echo "View traces in Zipkin: http://localhost:9411"
echo "View in API Flow Visualizer: http://localhost:5175/flows"
echo ""
echo "To enable chaos mode, restart services with:"
echo "  CHAOS_LATENCY_ENABLED=true docker-compose up -d"
echo "  CHAOS_ERROR_ENABLED=true CHAOS_ERROR_RATE=0.3 docker-compose up -d"

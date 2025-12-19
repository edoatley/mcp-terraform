#!/bin/bash
# Script to run API tests using Gradle
# These tests run against a running application instance (local or deployed)

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== API Test Runner ===${NC}"
echo ""

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# Default configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
GRPC_HOST="${GRPC_HOST:-localhost}"
GRPC_PORT="${GRPC_PORT:-9090}"

echo -e "${YELLOW}Configuration:${NC}"
echo "  API Base URL: $API_BASE_URL"
echo "  gRPC Host: $GRPC_HOST"
echo "  gRPC Port: $GRPC_PORT"
echo ""

# Check if application is running (optional check)
echo -e "${YELLOW}Checking if application is running...${NC}"
if curl -s -f "$API_BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Application is running at $API_BASE_URL${NC}"
elif [[ "$API_BASE_URL" == "http://localhost:8080" ]]; then
    echo -e "${YELLOW}⚠ Application does not appear to be running at $API_BASE_URL${NC}"
    echo -e "${YELLOW}  You may need to start the application first:${NC}"
    echo -e "${BLUE}    ./gradlew bootRun${NC}"
    echo -e "${YELLOW}  Or set API_BASE_URL to point to a deployed instance:${NC}"
    echo -e "${BLUE}    export API_BASE_URL=https://your-deployed-api.com${NC}"
    echo -e "${YELLOW}  Continuing anyway...${NC}"
else
    echo -e "${YELLOW}⚠ Could not verify application at $API_BASE_URL${NC}"
    echo -e "${YELLOW}  Continuing anyway (may be behind authentication or different health endpoint)...${NC}"
fi
echo ""

# Step 1: Build the project (if needed)
echo -e "${YELLOW}Step 1: Building project...${NC}"
if ./gradlew build -x test -x apiTest > /dev/null 2>&1; then
    echo "✓ Project built successfully"
else
    echo -e "${YELLOW}⚠ Build had issues, but continuing...${NC}"
fi
echo ""

# Step 2: Run API tests
echo -e "${YELLOW}Step 2: Running API tests...${NC}"
echo -e "${BLUE}  Target: $API_BASE_URL${NC}"
echo -e "${BLUE}  gRPC: $GRPC_HOST:$GRPC_PORT${NC}"
echo ""

# Export environment variables for the tests
export API_BASE_URL
export GRPC_HOST
export GRPC_PORT

if ./gradlew apiTest; then
    echo ""
    echo -e "${GREEN}✓ All API tests passed!${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}✗ API tests failed${NC}"
    echo ""
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo "  1. Ensure the application is running:"
    echo "     ./gradlew bootRun"
    echo ""
    echo "  2. Or set environment variables to point to a deployed instance:"
    echo "     export API_BASE_URL=https://your-api.com"
    echo "     export GRPC_HOST=your-grpc-host"
    echo "     export GRPC_PORT=9090"
    echo ""
    echo "  3. Check the test output above for specific failures"
    exit 1
fi


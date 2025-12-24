#!/bin/bash
# Script to run API tests using Gradle
# Automatically starts the application in the background for local testing,
# or uses a deployed instance if API_BASE_URL is set to a remote URL

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Cleanup function to kill background processes
cleanup() {
    if [[ -n "$APP_PID" ]]; then
        echo ""
        echo -e "${YELLOW}Cleaning up: Stopping application...${NC}"
        
        # Kill the Gradle process
        if kill -0 "$APP_PID" 2>/dev/null; then
            kill "$APP_PID" 2>/dev/null || true
        fi
        
        # Find and kill the Java process running TodoApplication
        # This is more reliable than killing the Gradle wrapper
        JAVA_PIDS=$(pgrep -f "TodoApplication" 2>/dev/null || true)
        if [[ -n "$JAVA_PIDS" ]]; then
            echo "  Stopping Java process(es): $JAVA_PIDS"
            kill $JAVA_PIDS 2>/dev/null || true
            sleep 2
            # Force kill if still running
            kill -9 $JAVA_PIDS 2>/dev/null || true
        fi
        
        # Also try to kill the Gradle process tree
        if kill -0 "$APP_PID" 2>/dev/null; then
            kill -9 "$APP_PID" 2>/dev/null || true
        fi
        
        echo -e "${GREEN}✓ Application stopped${NC}"
    fi
}

# Set trap to cleanup on exit
trap cleanup EXIT INT TERM

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

# Determine if we should start the app locally
START_APP_LOCALLY=false
if [[ "$API_BASE_URL" == "http://localhost:8080" ]] || [[ "$API_BASE_URL" == "http://127.0.0.1:8080" ]]; then
    START_APP_LOCALLY=true
fi

echo -e "${YELLOW}Configuration:${NC}"
echo "  API Base URL: $API_BASE_URL"
echo "  gRPC Host: $GRPC_HOST"
echo "  gRPC Port: $GRPC_PORT"
if [[ "$START_APP_LOCALLY" == "true" ]]; then
    echo "  Mode: Local (will start application automatically)"
else
    echo "  Mode: Remote (using deployed instance)"
fi
echo ""

# Step 1: Build the project
echo -e "${YELLOW}Step 1: Building project...${NC}"
if ./gradlew build -x test -x apiTest > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Project built successfully${NC}"
else
    echo -e "${YELLOW}⚠ Build had issues, but continuing...${NC}"
fi
echo ""

# Step 2: Start application if needed
APP_PID=""
if [[ "$START_APP_LOCALLY" == "true" ]]; then
    echo -e "${YELLOW}Step 2: Starting application in background...${NC}"
    
    # Check if app is already running
    if curl -s -f "$API_BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${YELLOW}⚠ Application is already running at $API_BASE_URL${NC}"
        echo -e "${YELLOW}  Using existing instance...${NC}"
    else
        # Start the application in background
        # Redirect output to a log file
        APP_LOG="$PROJECT_ROOT/app.log"
        echo "  Starting application (logs: $APP_LOG)..."
        
        # Start bootRun in background and capture PID
        ./gradlew bootRun > "$APP_LOG" 2>&1 &
        APP_PID=$!
        
        echo "  Application started (PID: $APP_PID)"
        echo "  Waiting for application to be ready..."
        
        # Wait for application to be ready (max 60 seconds)
        MAX_WAIT=60
        WAIT_COUNT=0
        while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
            if curl -s -f "$API_BASE_URL/actuator/health" > /dev/null 2>&1; then
                echo -e "${GREEN}✓ Application is ready!${NC}"
                break
            fi
            
            # Check if process is still running
            if ! kill -0 "$APP_PID" 2>/dev/null; then
                echo -e "${RED}✗ Application process died unexpectedly${NC}"
                echo "  Check logs: $APP_LOG"
                exit 1
            fi
            
            sleep 1
            WAIT_COUNT=$((WAIT_COUNT + 1))
            if [ $((WAIT_COUNT % 5)) -eq 0 ]; then
                echo "  Still waiting... (${WAIT_COUNT}s/${MAX_WAIT}s)"
            fi
        done
        
        if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
            echo -e "${RED}✗ Application did not become ready within ${MAX_WAIT} seconds${NC}"
            echo "  Check logs: $APP_LOG"
            exit 1
        fi
    fi
    echo ""
else
    echo -e "${YELLOW}Step 2: Checking remote application...${NC}"
    if curl -s -f "$API_BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Remote application is reachable${NC}"
    else
        echo -e "${YELLOW}⚠ Could not verify remote application at $API_BASE_URL${NC}"
        echo -e "${YELLOW}  Continuing anyway (may be behind authentication or different health endpoint)...${NC}"
    fi
    echo ""
fi

# Step 3: Run API tests
echo -e "${YELLOW}Step 3: Running API tests...${NC}"
echo -e "${BLUE}  Target: $API_BASE_URL${NC}"
echo -e "${BLUE}  gRPC: $GRPC_HOST:$GRPC_PORT${NC}"
echo ""

# Export environment variables for the tests
export API_BASE_URL
export GRPC_HOST
export GRPC_PORT

TEST_EXIT_CODE=0
if ./gradlew apiTest; then
    echo ""
    echo -e "${GREEN}✓ All API tests passed!${NC}"
    TEST_EXIT_CODE=0
else
    echo ""
    echo -e "${RED}✗ API tests failed${NC}"
    TEST_EXIT_CODE=1
fi

# Cleanup will happen automatically via trap
exit $TEST_EXIT_CODE


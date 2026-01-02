#!/bin/bash
set -euo pipefail

echo "Starting HashiCorp Terraform MCP Server..."

# Check if Docker is available
if ! command -v docker &> /dev/null; then
  echo "Warning: Docker not available, skipping MCP server startup"
  echo "mcp_available=false" >> $GITHUB_OUTPUT
  exit 0
fi

# Pull the latest MCP server image
echo "Pulling HashiCorp Terraform MCP Server image..."
if ! docker pull hashicorp/terraform-mcp-server:latest; then
  echo "Warning: Failed to pull MCP server image, continuing without MCP validation"
  echo "mcp_available=false" >> $GITHUB_OUTPUT
  exit 0
fi

# Test JSON-RPC stdio mode
echo "Testing MCP server JSON-RPC stdio mode..."
if echo '{"jsonrpc": "2.0", "id": "test", "method": "initialize", "params": {"protocolVersion": "2024-11-05", "clientInfo": {"name": "test", "version": "1.0.0"}}}' | timeout 15 docker run --rm -i hashicorp/terraform-mcp-server:latest 2>/dev/null | grep -q '"result"'; then
  echo "MCP server JSON-RPC stdio mode is working"
  echo "mcp_available=true" >> $GITHUB_OUTPUT
else
  echo "Warning: MCP server stdio mode test failed, continuing without MCP validation"
  echo "mcp_available=false" >> $GITHUB_OUTPUT
fi


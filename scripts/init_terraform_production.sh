#!/bin/bash
# Script to initialize Terraform with S3 backend for production deployments
# This switches from local backend (used for tests) to S3 backend

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(cd "$SCRIPT_DIR/../terraform" && pwd)"
BACKEND_S3_CONFIG="$TERRAFORM_DIR/backend-s3.hcl"

echo -e "${GREEN}=== Terraform Production Initialization ===${NC}"
echo ""

# Check if backend-s3.hcl exists
if [[ ! -f "$BACKEND_S3_CONFIG" ]]; then
  echo -e "${RED}Error: backend-s3.hcl not found!${NC}"
  echo ""
  echo "Please create it by copying the example:"
  echo "  cd terraform"
  echo "  cp backend-s3.hcl.example backend-s3.hcl"
  echo "  # Edit backend-s3.hcl with your S3 bucket and DynamoDB table names"
  exit 1
fi

# Check if backend block needs to be changed
cd "$TERRAFORM_DIR"

# Check current backend type
CURRENT_BACKEND=$(grep -A 1 'backend "' main.tf | head -1 | sed 's/.*backend "\([^"]*\)".*/\1/')

if [[ "$CURRENT_BACKEND" != "s3" ]]; then
  echo -e "${YELLOW}Switching backend from '$CURRENT_BACKEND' to 's3'...${NC}"
  
  # Create backup
  cp main.tf main.tf.backup
  
  # Replace backend block
  if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' '/backend "local"/,/}/c\
  backend "s3" {\
    # S3 backend configuration provided via -backend-config\
  }' main.tf
  else
    # Linux
    sed -i '/backend "local"/,/}/c\  backend "s3" {\n    # S3 backend configuration provided via -backend-config\n  }' main.tf
  fi
  
  echo "✓ Backend switched to S3 (backup saved as main.tf.backup)"
else
  echo "✓ Backend already configured for S3"
fi

# Set AWS SDK config for SSO support
export AWS_SDK_LOAD_CONFIG=1

# Initialize with S3 backend config
echo -e "${YELLOW}Initializing Terraform with S3 backend...${NC}"
if terraform init -backend-config="$BACKEND_S3_CONFIG" -migrate-state; then
  echo ""
  echo -e "${GREEN}✓ Terraform initialized with S3 backend${NC}"
  echo ""
  echo "You can now use:"
  echo "  terraform plan"
  echo "  terraform apply"
else
  echo -e "${RED}✗ Initialization failed${NC}"
  exit 1
fi




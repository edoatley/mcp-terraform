#!/bin/bash
# Script to switch Terraform back to local backend (for testing)
# This is useful after using S3 backend for production

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(cd "$SCRIPT_DIR/../terraform" && pwd)"

echo -e "${GREEN}=== Switching to Local Backend ===${NC}"
echo ""

cd "$TERRAFORM_DIR"

# Check current backend type
CURRENT_BACKEND=$(grep -A 1 'backend "' main.tf | head -1 | sed 's/.*backend "\([^"]*\)".*/\1/' || echo "unknown")

if [[ "$CURRENT_BACKEND" != "local" ]]; then
  echo -e "${YELLOW}Switching backend from '$CURRENT_BACKEND' to 'local'...${NC}"
  
  # Create backup
  cp main.tf main.tf.backup
  
  # Replace backend block
  if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' '/backend "s3"/,/}/c\
  backend "local" {\
    # Local backend for testing - no prompts required\
    # For production, use S3 backend with -backend-config flags\
    path = "terraform.tfstate"\
  }' main.tf
  else
    # Linux
    sed -i '/backend "s3"/,/}/c\  backend "local" {\n    # Local backend for testing - no prompts required\n    # For production, use S3 backend with -backend-config flags\n    path = "terraform.tfstate"\n  }' main.tf
  fi
  
  echo "✓ Backend switched to local (backup saved as main.tf.backup)"
else
  echo "✓ Backend already configured for local"
fi

# Initialize with local backend
echo -e "${YELLOW}Initializing Terraform with local backend...${NC}"
terraform init -migrate-state

echo ""
echo -e "${GREEN}✓ Terraform initialized with local backend${NC}"
echo ""
echo "You can now run tests with:"
echo "  ./scripts/run_terraform_tests.sh"



#!/bin/bash
# Script to run Terraform tests with clean state
# Ensures tests are rerunnable by cleaning local state files

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 0: Clean up any existing AWS credential environment variables
# This ensures SSO profiles are used instead of stale static credentials
echo -e "${GREEN}=== Terraform Test Runner ===${NC}"
echo ""
echo -e "${YELLOW}Step 0: Cleaning up AWS credential environment variables...${NC}"

# In CI environments (like GitHub Actions), preserve OIDC credentials
# Check if we're in CI and have OIDC credentials set
if [[ -n "$CI" ]] || [[ -n "$GITHUB_ACTIONS" ]]; then
  if [[ -n "$AWS_ACCESS_KEY_ID" ]] || [[ -n "$AWS_ROLE_ARN" ]]; then
    echo "✓ Running in CI with OIDC credentials - preserving credentials"
    # Don't unset credentials in CI when using OIDC
  else
    # CI but no OIDC credentials, proceed with cleanup
    unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN AWS_DEFAULT_REGION AWS_REGION
    echo "✓ Cleared AWS credential environment variables"
  fi
else
  # Local environment - always clean up to use SSO profiles
  unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN AWS_DEFAULT_REGION AWS_REGION
  echo "✓ Cleared AWS credential environment variables"
fi

# Default profile (export so Terraform inherits)
# Only set default profile if not in CI with OIDC
if [[ -z "$CI" ]] || [[ -z "$AWS_ACCESS_KEY_ID" ]]; then
  AWS_PROFILE="${AWS_PROFILE:-sandbox}"
  export AWS_PROFILE
  echo "✓ Using AWS profile: $AWS_PROFILE"
else
  echo "✓ Using OIDC credentials (no profile needed)"
fi
echo ""

# Step 1: Set AWS SDK config for SSO support
echo -e "${YELLOW}Step 1: Configuring AWS SDK for SSO...${NC}"
export AWS_SDK_LOAD_CONFIG=1
echo "✓ AWS_SDK_LOAD_CONFIG=1"

# Step 2: Login to AWS SSO (if using SSO profile)
if [[ -n "$AWS_PROFILE" ]]; then
  echo -e "${YELLOW}Step 2: Logging into AWS SSO (profile: $AWS_PROFILE)...${NC}"
  if aws sso login --profile "$AWS_PROFILE" 2>/dev/null; then
    echo "✓ Successfully logged into AWS SSO"
  else
    echo -e "${YELLOW}⚠ SSO login failed or not needed (may already be logged in)${NC}"
  fi
fi

# Step 3: Change to terraform directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(cd "$SCRIPT_DIR/../terraform" && pwd)"
cd "$TERRAFORM_DIR"
echo -e "${YELLOW}Step 3: Changed to terraform directory${NC}"
echo "  Working directory: $TERRAFORM_DIR"

# Step 4: Clean local state files
echo -e "${YELLOW}Step 4: Cleaning local state files...${NC}"
FILES_TO_CLEAN=(
  ".terraform"
  "terraform.tfstate"
  "terraform.tfstate.backup"
  ".terraform.lock.hcl"
)

CLEANED=false
for file in "${FILES_TO_CLEAN[@]}"; do
  if [[ -e "$file" ]] || [[ -d "$file" ]]; then
    echo "  Removing: $file"
    rm -rf "$file"
    CLEANED=true
  fi
done

if [[ "$CLEANED" == "false" ]]; then
  echo "  ✓ No state files to clean"
else
  echo "  ✓ State files cleaned"
fi

# Step 5: Initialize Terraform (with local backend)
echo -e "${YELLOW}Step 5: Initializing Terraform...${NC}"
terraform init
echo "✓ Terraform initialized"

# Step 6: Run tests
echo ""
echo -e "${YELLOW}Step 6: Running Terraform tests...${NC}"
echo ""

if terraform test; then
  echo ""
  echo -e "${GREEN}✓ All tests passed!${NC}"
  exit 0
else
  echo ""
  echo -e "${RED}✗ Tests failed${NC}"
  exit 1
fi


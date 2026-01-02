#!/bin/bash
set -euo pipefail

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Generating Terraform plan..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ -n "${AWS_ROLE_ARN:-}" ]; then
  echo "✓ AWS credentials configured via OIDC"
  echo "Generating full Terraform plan with AWS data sources..."
else
  echo "⚠️  AWS credentials not configured"
  echo "Plan may fail due to missing AWS credentials for data sources."
  echo "Provider validation will use fallback methods if plan fails."
fi
echo ""

PLAN_EXIT_CODE=0
terraform plan -out=tfplan.binary -no-color -refresh=false > plan_output.txt 2>&1 || PLAN_EXIT_CODE=$?

if [ $PLAN_EXIT_CODE -ne 0 ]; then
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "⚠️  Terraform plan failed (exit code: $PLAN_EXIT_CODE)"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  
  if [ -z "${AWS_ROLE_ARN:-}" ]; then
    echo "Plan failed because AWS credentials are not configured."
    echo "To enable full plan generation:"
    echo "  1. Set up AWS OIDC authentication (see README.md)"
    echo "  2. Configure AWS_ROLE_ARN secret in GitHub repository"
    echo ""
    echo "Provider validation will continue using fallback methods."
  else
    echo "Plan failed despite AWS credentials being configured."
    echo "This may indicate:"
    echo "  - IAM role permissions are insufficient"
    echo "  - AWS region configuration issue"
    echo "  - Network connectivity problem"
    echo ""
    echo "Provider validation will continue using fallback methods."
  fi
  
  echo ""
  echo "Last 20 lines of plan output:"
  echo "──────────────────────────────────────────────────────────────────────────────"
  tail -20 plan_output.txt || true
  echo "──────────────────────────────────────────────────────────────────────────────"
  echo "plan_successful=false" >> $GITHUB_OUTPUT
else
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "✓ Terraform plan generated successfully"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  
  # Show plan summary
  if grep -q "Plan:" plan_output.txt; then
    echo "Plan Summary:"
    echo "──────────────────────────────────────────────────────────────────────────────"
    grep -A 10 "Plan:" plan_output.txt | head -15 || true
    echo "──────────────────────────────────────────────────────────────────────────────"
  fi
  
  echo "plan_successful=true" >> $GITHUB_OUTPUT
fi

if [ -f tfplan.binary ]; then
  echo ""
  echo "Converting plan to JSON..."
  terraform show -json tfplan.binary > ../tfplan.json
  PLAN_SIZE=$(wc -c < ../tfplan.json)
  echo "✓ Plan JSON generated (${PLAN_SIZE} bytes)"
else
  echo ""
  echo "Plan binary not created. Creating empty JSON for fallback provider extraction..."
  echo "{}" > ../tfplan.json
fi


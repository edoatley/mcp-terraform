# Terraform Testing Guide

## Quick Start

Terraform tests require AWS credentials because the configuration uses data sources that query AWS (even though tests only run `terraform plan`, not `terraform apply`).

### Option 1: Use Test Script (Recommended)

The easiest way to run tests is using the provided script, which handles SSO login, state cleanup, and initialization:

```bash
# Set your AWS profile (optional, defaults to 'sandbox')
export AWS_PROFILE=sandbox

# Run the test script
./scripts/run_terraform_tests.sh
```

The script automatically:
- Sets `AWS_SDK_LOAD_CONFIG=1` for SSO support
- Logs into AWS SSO (if needed)
- Cleans local state files (ensures tests are rerunnable)
- Initializes Terraform with local backend
- Runs all tests

### Option 2: Manual Testing with AWS Profile

If you prefer to run tests manually:

```bash
export AWS_SDK_LOAD_CONFIG=1   # required for AWS SSO profiles
export AWS_PROFILE=sandbox
cd terraform
terraform init
terraform test
```

### Option 3: Use Default AWS Credentials

If you have default AWS credentials configured:

```bash
cd terraform
terraform init
terraform test
```

### Option 4: Use Environment Variables

```bash
export AWS_SDK_LOAD_CONFIG=1  # optional; safe to set for all methods
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_REGION=eu-west-2
cd terraform
terraform init
terraform test
```

## Setting Up AWS Profile

If you need to set up the sandbox profile:

```bash
aws configure --profile sandbox
```

You'll be prompted for:
- AWS Access Key ID
- AWS Secret Access Key
- Default region (e.g., `eu-west-2`)
- Default output format (can leave as default)

Then use it:
```bash
export AWS_PROFILE=sandbox
cd terraform
terraform test
```

## What Tests Do

The tests use `command = plan`, which means they:
- ✅ Validate your Terraform configuration
- ✅ Check resource naming and configuration
- ✅ Verify resource relationships
- ❌ **Do NOT create any AWS resources**
- ❌ **Do NOT modify any AWS resources**

However, they still need AWS credentials because:
- The configuration uses `data.aws_vpc.default` to find the default VPC
- The configuration uses `data.aws_subnets.default` to find subnets in that VPC
- These data sources require AWS API calls to read information

## Troubleshooting

### Error: "InvalidClientTokenId"

**Cause:** SSO session expired or Terraform cannot read your SSO profile.

**Solution:**
1. Re-login: `aws sso login --profile sandbox`
2. Ensure config is loaded: `export AWS_SDK_LOAD_CONFIG=1`
3. Re-run tests: `cd terraform && terraform test`

### Error: "No valid credential sources found"

**Solution:** Configure AWS credentials using one of the methods above.

### Error: "failed to get shared config profile, sandbox"

**Solution:** The profile doesn't exist. Either:
1. Create it: `aws configure --profile sandbox`
2. Or use default credentials: `unset AWS_PROFILE`
3. Or use environment variables instead

### Tests Still Fail After Setting Credentials

Check that your AWS credentials have permissions to:
- Read VPC information (`ec2:DescribeVpcs`)
- Read subnet information (`ec2:DescribeSubnets`)

These are read-only operations and should work with most AWS accounts.

## Running Specific Tests

```bash
# Run all tests
terraform test

# Run specific test file
terraform test tests/main.tftest.hcl
terraform test tests/lambda.tftest.hcl
terraform test tests/api_gateway.tftest.hcl
terraform test tests/alb.tftest.hcl
terraform test tests/outputs.tftest.hcl
terraform test tests/variables.tftest.hcl
```

## Test Coverage

The tests validate:
- ✅ S3 bucket configuration (versioning, encryption)
- ✅ DynamoDB tables configuration (state locking and todos)
- ✅ Todos DynamoDB table (PITR, encryption, billing mode)
- ✅ Lambda function and ECR repository
- ✅ Lambda IAM permissions (basic execution and DynamoDB access)
- ✅ Lambda environment variables (including DynamoDB table name and Spring profile)
- ✅ API Gateway resources and integrations
- ✅ Application Load Balancer and security groups
- ✅ Output values (including DynamoDB todos table name)
- ✅ Variable defaults and validation

See `tests/README.md` for detailed documentation.


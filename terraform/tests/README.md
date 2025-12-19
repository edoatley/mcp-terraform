# Terraform Tests

This directory contains Terraform test files that validate the infrastructure configuration.

## Test Files

- **main.tftest.hcl**: Tests for S3 bucket, DynamoDB table, and basic infrastructure
- **lambda.tftest.hcl**: Tests for Lambda function, ECR repository, and IAM roles
- **api_gateway.tftest.hcl**: Tests for API Gateway configuration and integrations
- **alb.tftest.hcl**: Tests for Application Load Balancer and gRPC configuration
- **outputs.tftest.hcl**: Tests for output values
- **variables.tftest.hcl**: Tests for variable validation and defaults

## Running Tests

### Prerequisites

- Terraform 1.6.0 or later (required for `terraform test` command)
- **AWS credentials configured** (required because tests use AWS data sources)
- If using AWS SSO, set `AWS_SDK_LOAD_CONFIG=1` so Terraform can read your SSO profile

### AWS Credentials Setup

Terraform tests require AWS credentials because the configuration uses data sources (`data.aws_vpc.default` and `data.aws_subnets.default`) that need to query AWS.

**Option 1: Use Test Script (Recommended)**

The easiest way to run tests is using the provided script:

```bash
# Set your AWS profile (optional, defaults to 'sandbox')
export AWS_PROFILE=sandbox

# Run the test script
./scripts/run_terraform_tests.sh
```

The script handles SSO login, state cleanup, initialization, and test execution automatically.

**Option 2: Manual Testing with AWS Profile**

If you prefer to run tests manually:

```bash
export AWS_SDK_LOAD_CONFIG=1   # required for AWS SSO profiles
export AWS_PROFILE=sandbox
cd terraform
terraform init
terraform test
```

**Option 3: Use Environment Variables**

```bash
export AWS_SDK_LOAD_CONFIG=1  # optional; safe to set for all methods
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_REGION=eu-west-2
cd terraform
terraform init
terraform test
```

**Option 4: Use Default AWS Credentials**

If you have default AWS credentials configured:

```bash
cd terraform
terraform init
terraform test
```

### Backend Configuration for Testing

The project is configured to use a **local backend by default** (no prompts required). The state file will be stored locally as `terraform.tfstate` in the `terraform` directory.

**For Testing:**
```bash
cd terraform
terraform init
terraform test
```

**For Production (S3 Backend):**

If you want to use S3 backend for production deployments:

1. Copy the example config:
   ```bash
   cp backend-s3.hcl.example backend-s3.hcl
   ```

2. Edit `backend-s3.hcl` with your S3 bucket and DynamoDB table names

3. Update `main.tf` to use S3 backend:
   ```hcl
   backend "s3" {
     # Backend configuration will be provided via -backend-config flags
   }
   ```

4. Initialize with S3 backend:
   ```bash
   terraform init -backend-config=backend-s3.hcl
   ```

### Run All Tests

```bash
cd terraform
terraform init
terraform test
```

### Run Specific Test File

```bash
terraform test tests/main.tftest.hcl
terraform test tests/lambda.tftest.hcl
terraform test tests/api_gateway.tftest.hcl
terraform test tests/alb.tftest.hcl
terraform test tests/outputs.tftest.hcl
terraform test tests/variables.tftest.hcl
```

### Run with Verbose Output

```bash
terraform test -verbose
```

### Run Tests in CI/CD

```bash
# Initialize Terraform
terraform init

# Run tests (will fail if any test fails)
terraform test
```

## Test Structure

Each test file contains one or more `run` blocks that define test scenarios:

```hcl
run "test_name" {
  command = plan  # or apply
  
  variables {
    # Test-specific variables
  }
  
  assert {
    condition     = <condition>
    error_message = "Error message if condition fails"
  }
}
```

## What Tests Validate

### Main Infrastructure
- S3 bucket creation with correct naming
- S3 bucket versioning enabled
- S3 bucket encryption (AES256)
- DynamoDB table creation with correct configuration
- DynamoDB billing mode (PAY_PER_REQUEST)
- DynamoDB hash key (LockID)

### Lambda Function
- ECR repository creation
- Image scanning enabled
- Lambda function configuration (memory, timeout)
- IAM role and policies
- Environment variables

### API Gateway
- API Gateway creation
- Resource paths (/todos, /todos/{id})
- HTTP methods (GET, POST, PUT, DELETE)
- Lambda integrations
- Stage configuration

### Application Load Balancer
- ALB creation and configuration
- Security group rules
- Target group configuration
- Health checks
- Lambda permissions

### Outputs
- All required outputs are defined
- Output values match expected resource names

### Variables
- Default values are correct
- Custom variables are applied correctly

## Notes

- Tests use `command = plan` to validate configuration without creating resources
- **Tests require AWS credentials** because they validate data sources (`data.aws_vpc.default`, `data.aws_subnets.default`)
- Tests can be run against any AWS account/region (they only read data, don't create resources)
- Test variables are isolated per test file
- If you see `InvalidClientTokenId`, re-run `aws sso login --profile <profile>` and ensure `AWS_SDK_LOAD_CONFIG=1` is set

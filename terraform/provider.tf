# AWS Provider configuration
# Can be overridden via environment variables or AWS CLI profiles

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile != "" ? var.aws_profile : null

  # Profile can be set via:
  # 1. Variable: terraform apply -var="aws_profile=sandbox"
  # 2. Environment: export AWS_PROFILE=sandbox
  # 3. AWS credentials file: ~/.aws/credentials
}


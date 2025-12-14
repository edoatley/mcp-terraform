# Backend configuration for Terraform state
# This file should be configured with actual values or use -backend-config flags

# Example backend configuration:
# terraform {
#   backend "s3" {
#     bucket         = "your-terraform-state-bucket"
#     key            = "mcp-terraform/terraform.tfstate"
#     region         = "eu-west-2"
#     dynamodb_table = "your-terraform-locks-table"
#     encrypt        = true
#   }
# }

# Note: For initial setup, you may need to:
# 1. Create the S3 bucket and DynamoDB table first (using main.tf)
# 2. Then configure the backend with those resource names
# 3. Run terraform init -migrate-state to migrate local state to S3


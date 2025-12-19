# Terraform tests for main infrastructure resources
# Tests S3 bucket, DynamoDB table, and basic configuration

variables {
  project_name = "mcp-terraform-todo-test"
  aws_region   = "eu-west-2"
  environment  = "test"
}

run "s3_bucket_created" {
  command = plan

  assert {
    condition     = aws_s3_bucket.terraform_state.bucket == "${var.project_name}-terraform-state"
    error_message = "S3 bucket name should match project name pattern"
  }
}

run "s3_bucket_versioning_enabled" {
  command = plan

  assert {
    condition     = aws_s3_bucket_versioning.terraform_state.versioning_configuration[0].status == "Enabled"
    error_message = "S3 bucket versioning should be enabled"
  }
}

run "s3_bucket_encryption_enabled" {
  command = plan

  assert {
    condition     = length([for rule in aws_s3_bucket_server_side_encryption_configuration.terraform_state.rule : rule if length(rule.apply_server_side_encryption_by_default) > 0 && rule.apply_server_side_encryption_by_default[0].sse_algorithm == "AES256"]) > 0
    error_message = "S3 bucket should have AES256 encryption enabled"
  }
}

run "dynamodb_table_created" {
  command = plan

  assert {
    condition     = aws_dynamodb_table.terraform_locks.name == "${var.project_name}-terraform-locks"
    error_message = "DynamoDB table name should match project name pattern"
  }
}

run "dynamodb_table_billing_mode" {
  command = plan

  assert {
    condition     = aws_dynamodb_table.terraform_locks.billing_mode == "PAY_PER_REQUEST"
    error_message = "DynamoDB table should use PAY_PER_REQUEST billing mode"
  }
}

run "dynamodb_table_hash_key" {
  command = plan

  assert {
    condition     = aws_dynamodb_table.terraform_locks.hash_key == "LockID"
    error_message = "DynamoDB table should have LockID as hash key"
  }
}




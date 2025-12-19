# Terraform tests for variable validation and defaults

run "default_variables" {
  command = plan

  # Test with default variables
  variables {
    project_name = "mcp-terraform-todo"
    aws_region   = "eu-west-2"
    environment  = "dev"
  }

  assert {
    condition     = var.project_name == "mcp-terraform-todo"
    error_message = "Default project_name should be mcp-terraform-todo"
  }

  assert {
    condition     = var.aws_region == "eu-west-2"
    error_message = "Default aws_region should be eu-west-2"
  }

  assert {
    condition     = var.environment == "dev"
    error_message = "Default environment should be dev"
  }
}

run "lambda_variables_defaults" {
  command = plan

  variables {
    project_name      = "mcp-terraform-todo"
    lambda_memory_size = 512
    lambda_timeout    = 30
  }

  assert {
    condition     = var.lambda_memory_size == 512
    error_message = "Default lambda_memory_size should be 512"
  }

  assert {
    condition     = var.lambda_timeout == 30
    error_message = "Default lambda_timeout should be 30"
  }
}

run "custom_variables" {
  command = plan

  variables {
    project_name      = "custom-project"
    aws_region        = "us-east-1"
    environment       = "prod"
    lambda_memory_size = 1024
    lambda_timeout    = 60
  }

  assert {
    condition     = var.project_name == "custom-project"
    error_message = "Custom project_name should be used"
  }

  assert {
    condition     = var.aws_region == "us-east-1"
    error_message = "Custom aws_region should be used"
  }

  assert {
    condition     = var.environment == "prod"
    error_message = "Custom environment should be used"
  }

  assert {
    condition     = var.lambda_memory_size == 1024
    error_message = "Custom lambda_memory_size should be used"
  }

  assert {
    condition     = var.lambda_timeout == 60
    error_message = "Custom lambda_timeout should be used"
  }
}




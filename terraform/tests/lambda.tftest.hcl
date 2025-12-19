# Terraform tests for Lambda function and related resources

variables {
  project_name      = "mcp-terraform-todo-test"
  aws_region        = "eu-west-2"
  environment       = "test"
  lambda_memory_size = 512
  lambda_timeout    = 30
}

run "ecr_repository_created" {
  command = plan

  assert {
    condition     = aws_ecr_repository.todo_api.name == "${var.project_name}-api"
    error_message = "ECR repository name should match project name pattern"
  }
}

run "ecr_image_scanning_enabled" {
  command = plan

  assert {
    condition     = aws_ecr_repository.todo_api.image_scanning_configuration[0].scan_on_push == true
    error_message = "ECR repository should have image scanning enabled"
  }
}

run "lambda_function_created" {
  command = plan

  assert {
    condition     = aws_lambda_function.todo_api.function_name == "${var.project_name}-api"
    error_message = "Lambda function name should match project name pattern"
  }
}

run "lambda_package_type" {
  command = plan

  assert {
    condition     = aws_lambda_function.todo_api.package_type == "Image"
    error_message = "Lambda function should use Image package type"
  }
}

run "lambda_memory_size" {
  command = plan

  assert {
    condition     = aws_lambda_function.todo_api.memory_size == var.lambda_memory_size
    error_message = "Lambda function memory size should match variable"
  }
}

run "lambda_timeout" {
  command = plan

  assert {
    condition     = aws_lambda_function.todo_api.timeout == var.lambda_timeout
    error_message = "Lambda function timeout should match variable"
  }
}

run "lambda_iam_role_created" {
  command = plan

  assert {
    condition     = aws_iam_role.lambda_role.name == "${var.project_name}-lambda-role"
    error_message = "Lambda IAM role name should match project name pattern"
  }
}

run "lambda_iam_role_assume_policy" {
  command = plan

  assert {
    condition     = jsondecode(aws_iam_role.lambda_role.assume_role_policy).Statement[0].Principal.Service == "lambda.amazonaws.com"
    error_message = "Lambda IAM role should allow Lambda service to assume it"
  }
}

run "lambda_basic_execution_role_attached" {
  command = plan

  assert {
    condition     = aws_iam_role_policy_attachment.lambda_basic.policy_arn == "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
    error_message = "Lambda should have basic execution role attached"
  }
}

run "lambda_environment_variables" {
  command = plan

  assert {
    condition     = aws_lambda_function.todo_api.environment[0].variables.ENVIRONMENT == var.environment
    error_message = "Lambda should have ENVIRONMENT variable set"
  }
}




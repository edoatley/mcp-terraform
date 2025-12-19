# Terraform tests for API Gateway configuration

variables {
  project_name = "mcp-terraform-todo-test"
  aws_region   = "eu-west-2"
  environment  = "test"
}

run "api_gateway_created" {
  command = plan

  assert {
    condition     = aws_api_gateway_rest_api.todo_api.name == "${var.project_name}-api"
    error_message = "API Gateway name should match project name pattern"
  }
}

run "api_gateway_endpoint_type" {
  command = plan

  assert {
    condition     = aws_api_gateway_rest_api.todo_api.endpoint_configuration[0].types[0] == "REGIONAL"
    error_message = "API Gateway should be REGIONAL endpoint type"
  }
}

run "api_gateway_todos_resource" {
  command = plan

  assert {
    condition     = aws_api_gateway_resource.todos.path_part == "todos"
    error_message = "API Gateway should have /todos resource"
  }
}

run "api_gateway_todo_resource" {
  command = plan

  assert {
    condition     = aws_api_gateway_resource.todo.path_part == "{id}"
    error_message = "API Gateway should have /todos/{id} resource"
  }
}

run "api_gateway_methods_created" {
  command = plan

  assert {
    condition = (
      aws_api_gateway_method.todos_get.http_method == "GET" &&
      aws_api_gateway_method.todos_post.http_method == "POST" &&
      aws_api_gateway_method.todo_get.http_method == "GET" &&
      aws_api_gateway_method.todo_put.http_method == "PUT" &&
      aws_api_gateway_method.todo_delete.http_method == "DELETE"
    )
    error_message = "All required API Gateway methods should be created"
  }
}

run "api_gateway_integrations_configured" {
  command = plan

  assert {
    condition = (
      aws_api_gateway_integration.todos_get.type == "AWS_PROXY" &&
      aws_api_gateway_integration.todos_post.type == "AWS_PROXY" &&
      aws_api_gateway_integration.todo_get.type == "AWS_PROXY" &&
      aws_api_gateway_integration.todo_put.type == "AWS_PROXY" &&
      aws_api_gateway_integration.todo_delete.type == "AWS_PROXY"
    )
    error_message = "All API Gateway integrations should be AWS_PROXY type"
  }
}

run "api_gateway_lambda_permission" {
  command = plan

  assert {
    condition     = aws_lambda_permission.api_gateway.principal == "apigateway.amazonaws.com"
    error_message = "Lambda permission should allow API Gateway to invoke"
  }
}

run "api_gateway_stage_created" {
  command = plan

  assert {
    condition     = aws_api_gateway_stage.todo_api.stage_name == var.environment
    error_message = "API Gateway stage name should match environment variable"
  }
}




# ECR Repository for Lambda container image
resource "aws_ecr_repository" "todo_api" {
  name                 = "${var.project_name}-api"
  image_tag_mutability = "MUTABLE"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# IAM role for Lambda function
resource "aws_iam_role" "lambda_role" {
  name = "${var.project_name}-lambda-role"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# IAM policy for DynamoDB access
resource "aws_iam_role_policy" "lambda_dynamodb" {
  name = "${var.project_name}-lambda-dynamodb-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Scan"
        ]
        Resource = aws_dynamodb_table.todos.arn
      }
    ]
  })
}

# Lambda function using container image
resource "aws_lambda_function" "todo_api" {
  function_name = "${var.project_name}-api"
  role          = aws_iam_role.lambda_role.arn
  package_type  = "Image"
  
  image_uri = "${aws_ecr_repository.todo_api.repository_url}:latest"
  
  timeout     = var.lambda_timeout
  memory_size = var.lambda_memory_size
  
  environment {
    variables = {
      ENVIRONMENT         = var.environment
      DYNAMODB_TABLE_NAME = aws_dynamodb_table.todos.name
      SPRING_PROFILES_ACTIVE = "dynamodb"
    }
  }
  
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# API Gateway for REST endpoints
resource "aws_api_gateway_rest_api" "todo_api" {
  name        = "${var.project_name}-api"
  description = "TODO API Gateway for REST endpoints"
  
  endpoint_configuration {
    types = ["REGIONAL"]
  }
  
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_api_gateway_resource" "todos" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  parent_id   = aws_api_gateway_rest_api.todo_api.root_resource_id
  path_part   = "todos"
}

resource "aws_api_gateway_resource" "todo" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  parent_id   = aws_api_gateway_resource.todos.id
  path_part   = "{id}"
}

# API Gateway methods
resource "aws_api_gateway_method" "todos_get" {
  rest_api_id   = aws_api_gateway_rest_api.todo_api.id
  resource_id   = aws_api_gateway_resource.todos.id
  http_method   = "GET"
  authorization  = "NONE"
}

resource "aws_api_gateway_method" "todos_post" {
  rest_api_id   = aws_api_gateway_rest_api.todo_api.id
  resource_id   = aws_api_gateway_resource.todos.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "todo_get" {
  rest_api_id   = aws_api_gateway_rest_api.todo_api.id
  resource_id   = aws_api_gateway_resource.todo.id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "todo_put" {
  rest_api_id   = aws_api_gateway_rest_api.todo_api.id
  resource_id   = aws_api_gateway_resource.todo.id
  http_method   = "PUT"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "todo_delete" {
  rest_api_id   = aws_api_gateway_rest_api.todo_api.id
  resource_id   = aws_api_gateway_resource.todo.id
  http_method   = "DELETE"
  authorization = "NONE"
}

# Lambda integrations for each method
resource "aws_api_gateway_integration" "todos_get" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  resource_id = aws_api_gateway_resource.todos.id
  http_method = aws_api_gateway_method.todos_get.http_method
  
  integration_http_method = "POST"
  type                     = "AWS_PROXY"
  uri                      = aws_lambda_function.todo_api.invoke_arn
}

resource "aws_api_gateway_integration" "todos_post" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  resource_id = aws_api_gateway_resource.todos.id
  http_method = aws_api_gateway_method.todos_post.http_method
  
  integration_http_method = "POST"
  type                     = "AWS_PROXY"
  uri                      = aws_lambda_function.todo_api.invoke_arn
}

resource "aws_api_gateway_integration" "todo_get" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  resource_id = aws_api_gateway_resource.todo.id
  http_method = aws_api_gateway_method.todo_get.http_method
  
  integration_http_method = "POST"
  type                     = "AWS_PROXY"
  uri                      = aws_lambda_function.todo_api.invoke_arn
}

resource "aws_api_gateway_integration" "todo_put" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  resource_id = aws_api_gateway_resource.todo.id
  http_method = aws_api_gateway_method.todo_put.http_method
  
  integration_http_method = "POST"
  type                     = "AWS_PROXY"
  uri                      = aws_lambda_function.todo_api.invoke_arn
}

resource "aws_api_gateway_integration" "todo_delete" {
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  resource_id = aws_api_gateway_resource.todo.id
  http_method = aws_api_gateway_method.todo_delete.http_method
  
  integration_http_method = "POST"
  type                     = "AWS_PROXY"
  uri                      = aws_lambda_function.todo_api.invoke_arn
}

# Permission for API Gateway to invoke Lambda
resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.todo_api.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.todo_api.execution_arn}/*/*"
}

# API Gateway deployment
resource "aws_api_gateway_deployment" "todo_api" {
  depends_on = [
    aws_api_gateway_integration.todos_get,
    aws_api_gateway_integration.todos_post,
    aws_api_gateway_integration.todo_get,
    aws_api_gateway_integration.todo_put,
    aws_api_gateway_integration.todo_delete,
  ]
  
  rest_api_id = aws_api_gateway_rest_api.todo_api.id
  
  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.todos.id,
      aws_api_gateway_resource.todo.id,
      aws_api_gateway_method.todos_get.id,
      aws_api_gateway_method.todos_post.id,
      aws_api_gateway_method.todo_get.id,
      aws_api_gateway_method.todo_put.id,
      aws_api_gateway_method.todo_delete.id,
    ]))
  }
  
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_api_gateway_stage" "todo_api" {
  deployment_id = aws_api_gateway_deployment.todo_api.id
  rest_api_id   = aws_api_gateway_rest_api.todo_api.id
  stage_name    = var.environment
}

# Application Load Balancer for gRPC
resource "aws_lb" "todo_grpc" {
  name               = "${var.project_name}-grpc-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = data.aws_subnets.default.ids
  
  enable_deletion_protection = false
  
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Security group for ALB
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb-sg"
  description = "Security group for Application Load Balancer"
  
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Target group for Lambda
resource "aws_lb_target_group" "todo_grpc" {
  name        = "${var.project_name}-grpc-tg"
  target_type = "lambda"
  
  health_check {
    enabled = true
    path    = "/health"
  }
  
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Attach Lambda to target group
resource "aws_lb_target_group_attachment" "todo_grpc" {
  target_group_arn = aws_lb_target_group.todo_grpc.arn
  target_id        = aws_lambda_function.todo_api.arn
  depends_on       = [aws_lambda_permission.alb]
}

# Permission for ALB to invoke Lambda
resource "aws_lambda_permission" "alb" {
  statement_id  = "AllowALBInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.todo_api.function_name
  principal     = "elasticloadbalancing.amazonaws.com"
  source_arn     = aws_lb_target_group.todo_grpc.arn
}

# ALB listener for gRPC
resource "aws_lb_listener" "todo_grpc" {
  load_balancer_arn = aws_lb.todo_grpc.arn
  port              = "80"
  protocol          = "HTTP"
  
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.todo_grpc.arn
  }
}

# Data source for default VPC subnets
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}


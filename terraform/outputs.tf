output "api_gateway_url" {
  description = "API Gateway endpoint URL"
  value       = "https://${aws_api_gateway_rest_api.todo_api.id}.execute-api.${var.aws_region}.amazonaws.com/${aws_api_gateway_stage.todo_api.stage_name}"
}

output "lambda_function_arn" {
  description = "Lambda function ARN"
  value       = aws_lambda_function.todo_api.arn
}

output "lambda_function_name" {
  description = "Lambda function name"
  value       = aws_lambda_function.todo_api.function_name
}

output "alb_dns_name" {
  description = "Application Load Balancer DNS name for gRPC"
  value       = aws_lb.todo_grpc.dns_name
}

output "terraform_state_bucket" {
  description = "S3 bucket for Terraform state"
  value       = aws_s3_bucket.terraform_state.id
}

output "terraform_lock_table" {
  description = "DynamoDB table for Terraform state locking"
  value       = aws_dynamodb_table.terraform_locks.name
}

output "dynamodb_todos_table_name" {
  description = "DynamoDB table name for Todos"
  value       = aws_dynamodb_table.todos.name
}


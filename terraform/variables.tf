variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "eu-west-2"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "mcp-terraform-todo"
}

variable "lambda_memory_size" {
  description = "Lambda function memory size in MB"
  type        = number
  default     = 512
}

variable "lambda_timeout" {
  description = "Lambda function timeout in seconds"
  type        = number
  default     = 30
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "aws_profile" {
  description = "AWS profile to use for authentication (optional, can also use AWS_PROFILE env var)"
  type        = string
  default     = ""
}


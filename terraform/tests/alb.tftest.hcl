# Terraform tests for Application Load Balancer and gRPC configuration

variables {
  project_name = "mcp-terraform-todo-test"
  aws_region   = "eu-west-2"
  environment  = "test"
}

run "alb_created" {
  command = plan

  assert {
    condition     = aws_lb.todo_grpc.name == "${var.project_name}-grpc-alb"
    error_message = "ALB name should match project name pattern"
  }
}

run "alb_application_type" {
  command = plan

  assert {
    condition     = aws_lb.todo_grpc.load_balancer_type == "application"
    error_message = "ALB should be application load balancer type"
  }
}

run "alb_external" {
  command = plan

  assert {
    condition     = aws_lb.todo_grpc.internal == false
    error_message = "ALB should be internet-facing (not internal)"
  }
}

run "security_group_created" {
  command = plan

  assert {
    condition     = aws_security_group.alb.name == "${var.project_name}-alb-sg"
    error_message = "Security group name should match project name pattern"
  }
}

run "security_group_ingress_rules" {
  command = plan

  # Check that ingress rules exist for ports 80 and 443
  assert {
    condition = (
      length([for rule in aws_security_group.alb.ingress : rule if rule.from_port == 80]) > 0 &&
      length([for rule in aws_security_group.alb.ingress : rule if rule.from_port == 443]) > 0
    )
    error_message = "Security group should have ingress rules for ports 80 and 443"
  }
}

run "target_group_created" {
  command = plan

  assert {
    condition     = aws_lb_target_group.todo_grpc.name == "${var.project_name}-grpc-tg"
    error_message = "Target group name should match project name pattern"
  }
}

run "target_group_type" {
  command = plan

  assert {
    condition     = aws_lb_target_group.todo_grpc.target_type == "lambda"
    error_message = "Target group should use Lambda target type"
  }
}

run "target_group_health_check" {
  command = plan

  assert {
    condition     = aws_lb_target_group.todo_grpc.health_check[0].enabled == true
    error_message = "Target group should have health check enabled"
  }
}

run "alb_listener_created" {
  command = plan

  assert {
    condition     = aws_lb_listener.todo_grpc.port == 80
    error_message = "ALB listener should listen on port 80"
  }
}

run "alb_listener_protocol" {
  command = plan

  assert {
    condition     = aws_lb_listener.todo_grpc.protocol == "HTTP"
    error_message = "ALB listener should use HTTP protocol"
  }
}

run "alb_lambda_permission" {
  command = plan

  assert {
    condition     = aws_lambda_permission.alb.principal == "elasticloadbalancing.amazonaws.com"
    error_message = "Lambda permission should allow ALB to invoke"
  }
}




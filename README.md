# Terraform MCP Server Test Setup

This project demonstrates how to use HashiCorp's Terraform MCP Server to validate Terraform plans in GitHub Actions. It includes a Spring Boot 4 TODO REST API with gRPC support, deployed to AWS Lambda via API Gateway.

## Architecture Overview

```
┌─────────────────┐
│  GitHub Actions │
│   Workflow      │
└────────┬────────┘
         │
         ├─► Terraform Init/Validate
         │
         ├─► MCP Server (Docker)
         │   └─► Validates Terraform plan
         │
         └─► PR Comments with Analysis
         
┌─────────────────────────────────────┐
│         AWS Infrastructure          │
│         (eu-west-2 London)         │
│                                     │
│  API Gateway ──► Lambda Function   │
│      │                  │          │
│      │                  └─► Spring │
│      │                      Boot 4  │
│      │                      TODO API│
│      │                      (REST + │
│      │                       gRPC)  │
│      └─► ALB (for gRPC)            │
└─────────────────────────────────────┘
```

## Features

- **MCP Validation**: Validates Terraform plans against HashiCorp's official registry
- **Spring Boot 4**: Modern Java framework with Lambda support
- **Dual API Support**: REST endpoints via API Gateway and gRPC endpoints via ALB
- **DevContainer Setup**: Local development environment with Java 21 pre-configured
- **Infrastructure as Code**: Complete AWS setup via Terraform (eu-west-2 region)
- **CI/CD Integration**: Automated validation in GitHub Actions
- **State Management**: S3 backend with DynamoDB locking

## Prerequisites

- Java 21+
- Maven 3.9+
- Terraform 1.13.4+
- Docker (for MCP server and local testing)
- AWS CLI configured with appropriate credentials
- VS Code with Dev Containers extension (for devcontainer setup)

## Project Structure

```
mcp-terraform/
├── src/                          # Spring Boot application
│   └── main/
│       ├── java/
│       │   └── com/example/todo/
│       │       ├── TodoApplication.java
│       │       ├── LambdaHandler.java
│       │       ├── controller/
│       │       │   ├── TodoController.java      # REST endpoints
│       │       │   └── TodoGrpcController.java   # gRPC service
│       │       ├── model/
│       │       │   └── Todo.java
│       │       ├── proto/                       # Protocol buffers
│       │       │   └── todo.proto
│       │       └── service/
│       │           └── TodoService.java
│       └── resources/
│           └── application.properties
├── terraform/
│   ├── main.tf                   # Main infrastructure
│   ├── variables.tf              # Variable definitions (region: eu-west-2)
│   ├── outputs.tf                # Output values
│   ├── backend.tf                # S3 backend config
│   └── lambda.tf                 # Lambda + API Gateway
├── .github/
│   └── workflows/
│       └── terraform-mcp.yml     # MCP validation workflow
├── .devcontainer/
│   ├── devcontainer.json         # Dev container configuration
│   └── Dockerfile                # Dev container image (Java 21)
├── scripts/
│   └── validate_terraform.py     # MCP validation script
├── Dockerfile                    # Lambda container image
├── pom.xml                       # Maven dependencies
├── tfplan.json                   # Sample Terraform plan (for testing)
└── README.md                     # This file
```

## DevContainer Setup

This project includes a VS Code devcontainer configuration for local development with Java 21.

### Using DevContainer

1. **Open in VS Code**: Open this project in VS Code
2. **Reopen in Container**: When prompted, click "Reopen in Container", or use Command Palette (`Cmd+Shift+P` / `Ctrl+Shift+P`) and select "Dev Containers: Reopen in Container"
3. **Wait for Setup**: The container will build and install all dependencies
4. **Start Development**: Once ready, you can build and run the application

### DevContainer Features

- Java 21 runtime pre-installed
- Maven 3.9 for building
- VS Code extensions for Java, Spring Boot, and gRPC
- Port forwarding for REST (8080) and gRPC (9090)
- grpcurl installed for testing gRPC endpoints

### Building in DevContainer

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

## Local Development

### Building the Application

```bash
# Build the project
mvn clean install

# Run the application locally
mvn spring-boot:run
```

The application will start on:
- REST API: http://localhost:8080
- gRPC: localhost:9090

### Testing REST API

```bash
# Get all todos
curl http://localhost:8080/api/todos

# Create a todo
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title": "Test Todo", "description": "This is a test"}'

# Get a specific todo (replace {id} with actual ID)
curl http://localhost:8080/api/todos/{id}

# Update a todo
curl -X PUT http://localhost:8080/api/todos/{id} \
  -H "Content-Type: application/json" \
  -d '{"title": "Updated Todo", "description": "Updated description", "completed": true}'

# Delete a todo
curl -X DELETE http://localhost:8080/api/todos/{id}
```

### Testing gRPC API

Using grpcurl (installed in devcontainer):

```bash
# List all todos
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext localhost:9090 com.example.todo.TodoService/ListTodos

# Create a todo
grpcurl -plaintext -d '{"title": "Test Todo", "description": "Test description"}' \
  localhost:9090 com.example.todo.TodoService/CreateTodo

# Get a todo
grpcurl -plaintext -d '{"id": "{todo-id}"}' \
  localhost:9090 com.example.todo.TodoService/GetTodo
```

## Terraform Deployment

### Initial Setup

1. **Configure AWS Credentials**:
   ```bash
   aws configure
   ```

2. **Initialize Terraform** (first time only):
   ```bash
   cd terraform
   terraform init
   ```

3. **Review the Plan**:
   ```bash
   terraform plan
   ```

4. **Apply Infrastructure**:
   ```bash
   terraform apply
   ```

### Backend Configuration

The Terraform backend uses S3 for state storage and DynamoDB for locking. Before first use:

1. The S3 bucket and DynamoDB table are created by `main.tf`
2. After first apply, update `terraform/backend.tf` with the actual bucket and table names
3. Re-run `terraform init -migrate-state` to migrate local state to S3

### Variables

Key variables (defined in `terraform/variables.tf`):

- `aws_region`: AWS region (default: `eu-west-2` - London)
- `project_name`: Project name for resource naming (default: `mcp-terraform-todo`)
- `lambda_memory_size`: Lambda memory in MB (default: `512`)
- `lambda_timeout`: Lambda timeout in seconds (default: `30`)
- `environment`: Environment name (default: `dev`)

### Outputs

After deployment, Terraform will output:

- `api_gateway_url`: REST API endpoint URL
- `lambda_function_arn`: Lambda function ARN
- `alb_dns_name`: Application Load Balancer DNS for gRPC
- `terraform_state_bucket`: S3 bucket for Terraform state
- `terraform_lock_table`: DynamoDB table for state locking

## GitHub Actions MCP Validation

The project includes a GitHub Actions workflow that validates Terraform plans using HashiCorp's MCP Server.

### Workflow Features

1. **Terraform Validation**: Runs `terraform init`, `validate`, and `fmt -check`
2. **MCP Server Integration**: Starts HashiCorp Terraform MCP Server in Docker
3. **Plan Analysis**: Extracts providers and validates against the registry
4. **PR Comments**: Posts validation results as PR comments

### How It Works

1. **MCP Server Startup**: The workflow checks for Docker and starts the MCP server
2. **Plan Generation**: Generates a Terraform plan and converts it to JSON
3. **Provider Extraction**: Identifies all providers used in the plan
4. **Registry Validation**: 
   - Checks latest provider versions
   - Searches for recommended modules
   - Retrieves resource documentation
5. **Report Generation**: Creates a validation report and AI analysis
6. **PR Comment**: Posts results to the pull request

### Workflow Triggers

The workflow runs on:
- Pull requests that modify Terraform files
- Pushes to main branch that modify Terraform files

### Manual Testing

You can test the MCP validation script locally:

```bash
# Ensure Docker is running
docker pull hashicorp/terraform-mcp-server:latest

# Generate a Terraform plan
cd terraform
terraform plan -out=tfplan.binary
terraform show -json tfplan.binary > ../tfplan.json

# Run validation script
cd ..
python scripts/validate_terraform.py
```

## API Documentation

### REST API Endpoints

Base URL: `https://{api-gateway-url}/{stage}/api/todos`

- `GET /api/todos` - Get all todos
- `GET /api/todos/{id}` - Get a specific todo
- `POST /api/todos` - Create a new todo
- `PUT /api/todos/{id}` - Update a todo
- `DELETE /api/todos/{id}` - Delete a todo

### gRPC API

Service: `com.example.todo.TodoService`

Methods:
- `GetTodo(GetTodoRequest) returns (GetTodoResponse)`
- `ListTodos(ListTodosRequest) returns (ListTodosResponse)`
- `CreateTodo(CreateTodoRequest) returns (CreateTodoResponse)`
- `UpdateTodo(UpdateTodoRequest) returns (UpdateTodoResponse)`
- `DeleteTodo(DeleteTodoRequest) returns (DeleteTodoResponse)`

See `src/main/proto/todo.proto` for complete protocol buffer definitions.

## Troubleshooting

### DevContainer Issues

- **Container won't start**: Ensure Docker is running and VS Code Dev Containers extension is installed
- **Port forwarding not working**: Check VS Code port forwarding settings
- **Maven build fails**: Try `mvn clean install -U` to update dependencies

### Terraform Issues

- **Backend initialization fails**: Ensure S3 bucket and DynamoDB table exist (created by `main.tf`)
- **Provider authentication errors**: Verify AWS credentials are configured
- **Resource creation fails**: Check AWS service limits and IAM permissions

### MCP Validation Issues

- **Docker not available**: The workflow will continue without MCP validation
- **MCP server timeout**: Check Docker daemon is running and has network access
- **Plan parsing errors**: Ensure `tfplan.json` is valid JSON

## Contributing

1. Create a feature branch
2. Make your changes
3. Ensure Terraform validation passes
4. Create a pull request
5. The MCP validation workflow will automatically run

## Resources

- [HashiCorp Terraform MCP Server](https://github.com/hashicorp/terraform-mcp-server)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Spring Boot 4 Documentation](https://spring.io/projects/spring-boot)
- [AWS Lambda with Spring Boot](https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)

## License

This project is provided as-is for demonstration purposes.


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
│         (eu-west-2 London)          │
│                                     │
│  API Gateway ──► Lambda Function    │
│      │                  │           │
│      │                  └─► Spring  │
│      │                      Boot 4  │
│      │                      TODO API│
│      │                      (REST + │
│      │                       gRPC)  │
│      └─► ALB (for gRPC)             │
└─────────────────────────────────────┘
```

## Features

- **MCP Validation**: Validates Terraform plans against HashiCorp's official registry
- **Spring Boot 4**: Modern Java framework with Lambda support
- **Dual API Support**: REST endpoints via API Gateway and gRPC endpoints via ALB
- **Infrastructure as Code**: Complete AWS setup via Terraform (eu-west-2 region)
- **CI/CD Integration**: Automated validation in GitHub Actions
- **State Management**: S3 backend with DynamoDB locking

## Prerequisites

- Java 21+
- Maven 3.9+
- Terraform 1.13.4+
- Docker (for MCP server and local testing)
- AWS CLI configured with appropriate credentials

## Local Tooling Requirements

- Java 21+ runtime and development tools
- Maven 3.9+ for building and running tests
- Terraform 1.13.4+ for infrastructure workflows
- Docker for running the MCP server locally
- AWS CLI with configured credentials for Terraform tests
- Python 3.11+ for `scripts/validate_terraform.py` and related tooling

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
├── scripts/
│   └── validate_terraform.py     # MCP validation script
├── Dockerfile                    # Lambda container image
├── pom.xml                       # Maven dependencies
├── tfplan.json                   # Sample Terraform plan (for testing)
└── README.md                     # This file
```


## Local Development

### Building the Application

```bash
# Build the project
mvn clean install

# Or using Gradle:
./gradlew build
```

### Running the Application Locally

The application supports two storage backends via Spring profiles:

**In-Memory Storage (Default for Local Development):**
```bash
# Using Maven:
./mvnw spring-boot:run

# Using Gradle:
./gradlew bootRun

# Or explicitly set the profile:
./mvnw spring-boot:run -Dspring.profiles.active=in-memory
```

**DynamoDB Storage:**
```bash
# Requires DYNAMODB_TABLE_NAME environment variable
export DYNAMODB_TABLE_NAME=mcp-terraform-todo-todos
./mvnw spring-boot:run -Dspring.profiles.active=dynamodb

# Or with DynamoDB Local:
export DYNAMODB_TABLE_NAME=mcp-terraform-todo-todos
export SPRING_PROFILES_ACTIVE=dynamodb
# Set dynamodb.endpoint=http://localhost:8000 in application.properties
./mvnw spring-boot:run
```

The application will start on:
- REST API: http://localhost:8080
- gRPC: localhost:9090

**Note:** By default, the application uses `in-memory` profile for local development, which requires no AWS setup. For production/Lambda deployments, use the `dynamodb` profile.

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

### Running API Tests

The project includes portable API tests that can run against local or deployed instances. These tests use REST Assured for REST endpoints and gRPC clients for gRPC endpoints.

#### Configuration

API tests are configured via `src/test/resources/application-api-test.properties` or environment variables:

**For Local Testing:**
```properties
api.base.url=http://localhost:8080
grpc.host=localhost
grpc.port=9090
```

**For Deployed Testing:**
```properties
api.base.url=https://your-api-gateway-url.execute-api.eu-west-2.amazonaws.com/prod
grpc.host=your-alb-endpoint.eu-west-2.elb.amazonaws.com
grpc.port=9090
```

#### Running Tests

**Quick Start (Recommended - Uses Test Script):**
```bash
# The test scripts automatically start the application, run tests, and clean up:
# Using Gradle:
./scripts/run_api_tests.sh

# Using Maven:
./scripts/run_api_tests_maven.sh
```

The test scripts automatically:
- Build the project
- Start the application in the background (for local testing)
- Wait for the application to be ready
- Run API tests with proper configuration
- Stop the application when tests complete
- Provide helpful error messages and troubleshooting tips

**Note:** For local testing, the scripts automatically start the application with the `in-memory` profile (no DynamoDB required). For testing against deployed instances, set `API_BASE_URL` to point to your deployed API.

**Against Local Instance (Manual):**
The test scripts automatically start the application, but if you prefer to start it manually:

1. Start the application with in-memory profile (no DynamoDB required):
   ```bash
   # Using Maven:
   ./mvnw spring-boot:run
   
   # Using Gradle:
   ./gradlew bootRun
   ```
   The application defaults to `in-memory` profile for local development.

2. Run API tests (in another terminal):
   - **Using script (Gradle)**: `./scripts/run_api_tests.sh` (will detect running app)
   - **Using script (Maven)**: `./scripts/run_api_tests_maven.sh` (will detect running app)
   - **Gradle**: `./gradlew apiTest`
   - **Maven**: `./mvnw test -PapiTest` or `./mvnw test -Dtest=TodoRestApiTest,TodoGrpcApiTest`

**Against Deployed Instance:**
1. Set environment variables or update `src/apiTest/resources/application-api-test.properties` with deployed URLs
2. Run API tests:
   - **Using script (Gradle)**: `export API_BASE_URL=https://your-api.com && ./scripts/run_api_tests.sh`
   - **Using script (Maven)**: `export API_BASE_URL=https://your-api.com && ./scripts/run_api_tests_maven.sh`
   - **Gradle**: `./gradlew apiTest`
   - **Maven**: `./mvnw test -PapiTest` or `./mvnw test -Dtest=TodoRestApiTest,TodoGrpcApiTest`

**Using Environment Variables:**
```bash
export API_BASE_URL=https://your-api-gateway-url.execute-api.eu-west-2.amazonaws.com/prod
export GRPC_HOST=your-alb-endpoint.eu-west-2.elb.amazonaws.com
export GRPC_PORT=9090

# Gradle
./gradlew apiTest

# Maven
mvn test -PapiTest
```

**Using System Properties:**
```bash
# Gradle
./gradlew apiTest -Dapi.base.url=https://your-api-gateway-url.execute-api.eu-west-2.amazonaws.com/prod \
  -Dgrpc.host=your-alb-endpoint.eu-west-2.elb.amazonaws.com \
  -Dgrpc.port=9090

# Maven
mvn test -PapiTest \
  -Dapi.base.url=https://your-api-gateway-url.execute-api.eu-west-2.amazonaws.com/prod \
  -Dgrpc.host=your-alb-endpoint.eu-west-2.elb.amazonaws.com \
  -Dgrpc.port=9090
```

**Note:** API tests are in a separate source set (`src/apiTest`) and have their own Gradle task (`apiTest`). They are excluded from the regular `test` task to allow running them independently against a running application instance.

The API tests cover:
- REST API: Full CRUD operations, error handling, workflow tests
- gRPC API: Full CRUD operations, error handling, workflow tests

**Note:** These tests are portable and can run against a running application instance. When using the test scripts (`run_api_tests.sh` or `run_api_tests_maven.sh`), the application is automatically started in the background. They do not start the Spring Boot application context (unlike integration tests).

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

### Running Terraform Tests

The project includes comprehensive Terraform test files that validate infrastructure configuration.

**Prerequisites:**
- Terraform 1.6.0 or later (required for `terraform test` command)
- **AWS credentials configured** (required because tests use AWS data sources)
- AWS SSO profile configured (if using SSO)

**Important:** Tests require AWS credentials because the configuration uses data sources (`data.aws_vpc.default` and `data.aws_subnets.default`) that query AWS. Tests only run `terraform plan` (they don't create resources), but they still need to read AWS information.

**Quick Start (Recommended - Uses Test Script):**
```bash
# Set your AWS profile (defaults to 'sandbox' if not set)
export AWS_PROFILE=sandbox  # optional, defaults to 'sandbox'

# Run the test script (handles SSO login, cleans state, runs tests)
./scripts/run_terraform_tests.sh
```

The test script automatically:
- Sets `AWS_SDK_LOAD_CONFIG=1` for SSO support
- Logs into AWS SSO (if needed)
- Cleans local state files (`.terraform/`, `terraform.tfstate*`, `.terraform.lock.hcl`)
- Initializes Terraform with local backend
- Runs all tests

**Manual Testing (Alternative):**
```bash
export AWS_SDK_LOAD_CONFIG=1      # required for AWS SSO profiles
export AWS_PROFILE=sandbox  # or your AWS profile name
cd terraform
terraform init
terraform test
```

**Using Default Credentials:**
```bash
cd terraform
terraform init
terraform test
```

**Using Environment Variables:**
```bash
export AWS_SDK_LOAD_CONFIG=1  # optional; safe to set for all methods
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_REGION=eu-west-2
cd terraform
terraform init
terraform test
```

See `terraform/TESTING.md` for detailed setup instructions and troubleshooting.

**Run Specific Test File:**
```bash
terraform test tests/main.tftest.hcl
terraform test tests/lambda.tftest.hcl
terraform test tests/api_gateway.tftest.hcl
terraform test tests/alb.tftest.hcl
terraform test tests/outputs.tftest.hcl
terraform test tests/variables.tftest.hcl
```

**Run with Verbose Output:**
```bash
terraform test -verbose
```

**Test Coverage:**
- S3 bucket configuration (versioning, encryption)
- DynamoDB table configuration
- Lambda function and ECR repository
- API Gateway resources and integrations
- Application Load Balancer and security groups
- Output values
- Variable defaults and validation

See `terraform/tests/README.md` for detailed test documentation.

**Troubleshooting common auth errors:**
- `InvalidClientTokenId`: SSO session expired or not loaded by Terraform. Run `aws sso login --profile <profile>`, ensure `AWS_SDK_LOAD_CONFIG=1`, and re-run tests.
- `failed to get shared config profile`: Profile name is wrong or missing. Create it with `aws configure --profile <profile>` or unset `AWS_PROFILE` to use defaults.

### Backend Configuration

The Terraform configuration supports two backend modes:

**Local Backend (Default - For Testing):**
- Used automatically by the test script
- State stored in `terraform/terraform.tfstate`
- No additional configuration needed
- Perfect for running tests without affecting production state

**S3 Backend (For Production Deployments):**
- State stored in S3 with DynamoDB locking
- Requires switching backend configuration

**Switching to S3 Backend for Production:**

1. **Create backend configuration file:**
   ```bash
   cd terraform
   cp backend-s3.hcl.example backend-s3.hcl
   # Edit backend-s3.hcl with your S3 bucket and DynamoDB table names
   ```

2. **Run the production init script:**
   ```bash
   ./scripts/init_terraform_production.sh
   ```
   
   This script:
   - Switches `main.tf` backend from `local` to `s3`
   - Initializes Terraform with S3 backend configuration
   - Migrates existing state (if any) to S3

3. **After production work, switch back to local for testing:**
   ```bash
   ./scripts/init_terraform_local.sh
   ```

**Manual Backend Switching:**

If you prefer to switch manually, edit `terraform/main.tf`:
- **For tests:** Use `backend "local"` block
- **For production:** Use `backend "s3"` block and run `terraform init -backend-config=backend-s3.hcl -migrate-state`

**Note:** The S3 bucket and DynamoDB table are created by `main.tf` resources. After first `terraform apply`, update `backend-s3.hcl` with the actual resource names.

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
- `dynamodb_todos_table_name`: DynamoDB table name for Todos (automatically set as Lambda environment variable)

## Test Coverage

The project uses JaCoCo for test coverage reporting with a target of 80%+ coverage.

### Generating Coverage Reports

#### Using Maven

```bash
# Run tests and generate coverage report
./mvnw clean test jacoco:report

# View HTML report
open target/site/jacoco/index.html

# Check coverage thresholds (fails if below 80%)
./mvnw clean test jacoco:check
```

#### Using Gradle

```bash
# Run tests and generate coverage report
./gradlew clean test jacocoTestReport

# View HTML report
open build/reports/jacoco/test/html/index.html

# Check coverage thresholds (fails if below 80%)
./gradlew jacocoTestCoverageVerification
```

### Coverage Exclusions

The following classes are excluded from coverage calculations:
- `TodoApplication` - Spring Boot main class
- `LambdaHandler` - AWS Lambda entry point
- `com.example.todo.model.*` - Data model classes (Lombok generated)
- `com.example.todo.proto.*` - Generated Protocol Buffer classes

### Coverage Reports Location

- **Maven**: `target/site/jacoco/index.html`
- **Gradle**: `build/reports/jacoco/test/html/index.html`

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

## TODO

This section contains a comprehensive list of tasks and improvements to be completed.

### Testing & Quality Assurance

- [x] **Unit Tests**: Add JUnit tests for `TodoService`, `TodoController`, and `TodoGrpcController`
- [x] **Integration Tests**: Create integration tests for REST and gRPC endpoints
- [x] **Test Coverage**: Set up JaCoCo or similar tool to track test coverage (target: 80%+)
- [x] **Contract Testing**: Add contract tests for gRPC service definitions
- [x] **API Testing**: Add portable REST Assured tests for local and deployed environments
- [x] **Terraform Testing**: Add `terraform test` files for infrastructure validation

### Data Persistence

- [x] **DynamoDB Integration**: Replace in-memory storage with DynamoDB for persistence
  - [x] Create DynamoDB table via Terraform
  - [x] Add DynamoDB SDK dependency
  - [x] Implement DynamoDB repository layer
  - [x] Add data migration scripts if needed (not required - no existing data)
- [x] **Database Schema**: Design and document data model
- [x] **Connection Pooling**: Configure proper connection pooling for database
- [x] **Data Validation**: Add input validation and sanitization

#### DynamoDB Table Schema

The Todos table uses a simple design optimized for the current access patterns:

**Table Name**: `${project_name}-todos` (e.g., `mcp-terraform-todo-todos`)

**Partition Key**: `id` (String) - UUID generated for each todo

**Attributes**:
- `id` (String, required) - Unique identifier, partition key
- `title` (String, required) - Todo title, max 200 characters
- `description` (String, optional) - Todo description, max 500 characters
- `completed` (Boolean) - Completion status, defaults to false

**Table Configuration**:
- **Billing Mode**: `PAY_PER_REQUEST` (on-demand) - Cost-effective for variable workloads
- **Point-in-Time Recovery**: Enabled for data protection
- **Encryption**: Server-side encryption with AWS managed keys
- **Region**: `eu-west-2` (London)

**Access Patterns**:
- Get todo by ID: Direct lookup using partition key (`id`)
- List all todos: Full table scan (efficient for small datasets)
- Create/Update/Delete: Direct operations using partition key

**Future Enhancements** (if needed):
- Global Secondary Index (GSI) for querying by `completed` status
- GSI for date-based queries if adding timestamps
- Pagination support for large datasets

#### Connection Pooling

DynamoDB uses HTTP connections managed by the AWS SDK v2. The SDK automatically handles:
- Connection reuse and pooling
- Request retries with exponential backoff
- Connection lifecycle management

No traditional database connection pooling is needed. The SDK efficiently manages HTTP connections to DynamoDB's REST API.

#### Spring Profiles

The application supports two storage backends via Spring profiles:

**In-Memory Profile (`in-memory`):**
- Uses `ConcurrentHashMap` for storage
- No AWS setup required
- Perfect for local development and testing
- Default profile for local development
- Activated with: `spring.profiles.active=in-memory`

**DynamoDB Profile (`dynamodb`):**
- Uses AWS DynamoDB for persistence
- Requires `DYNAMODB_TABLE_NAME` environment variable
- Used in production/Lambda deployments
- Default when no profile is specified (for Lambda)
- Activated with: `spring.profiles.active=dynamodb`

**Profile Selection:**
- Local development: Defaults to `in-memory` (no configuration needed)
- Lambda/Production: Set `SPRING_PROFILES_ACTIVE=dynamodb` environment variable
- Tests: Use `test,in-memory` profile (configured in `application-test.properties`)

**Switching Profiles:**
```bash
# Use in-memory (local development)
./mvnw spring-boot:run

# Use DynamoDB (requires AWS setup)
export DYNAMODB_TABLE_NAME=mcp-terraform-todo-todos
./mvnw spring-boot:run -Dspring.profiles.active=dynamodb
```

#### Data Validation

Input validation is implemented using Bean Validation (JSR-303/Jakarta Validation):

**REST API**:
- Validation annotations on `Todo` model (`@NotBlank`, `@Size`)
- Automatic validation via `@Valid` annotation on request bodies
- Validation errors return HTTP 400 with detailed error messages

**gRPC API**:
- Manual validation in `TodoGrpcController` for request fields
- Returns `INVALID_ARGUMENT` status for validation failures

**Validation Rules**:
- `title`: Required, not blank, max 200 characters
- `description`: Optional, max 500 characters
- `id`: Required for updates (validated by service layer)

#### Environment Variables

The following environment variables are required for DynamoDB:

- `DYNAMODB_TABLE_NAME`: DynamoDB table name (set automatically by Terraform)
- `AWS_REGION`: AWS region (defaults to `eu-west-2`)

For local development with DynamoDB Local:
- `dynamodb.endpoint`: Optional endpoint URL (e.g., `http://localhost:8000`)

#### Local Development with DynamoDB Local

To run the application locally with DynamoDB Local:

1. **Start DynamoDB Local** (using Docker):
   ```bash
   docker run -p 8000:8000 amazon/dynamodb-local
   ```

2. **Create the table** (using AWS CLI):
   ```bash
   aws dynamodb create-table \
     --table-name mcp-terraform-todo-todos \
     --attribute-definitions AttributeName=id,AttributeType=S \
     --key-schema AttributeName=id,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST \
     --endpoint-url http://localhost:8000
   ```

3. **Configure application** (`src/main/resources/application.properties`):
   ```properties
   dynamodb.endpoint=http://localhost:8000
   DYNAMODB_TABLE_NAME=mcp-terraform-todo-todos
   aws.region=us-east-1
   ```

4. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

### CI/CD & Deployment

- [ ] **Lambda Build Pipeline**: Create GitHub Actions workflow to build and push Docker image to ECR
  - [ ] Build Lambda container image
  - [ ] Push to ECR repository
  - [ ] Update Lambda function with new image
- [ ] **Automated Testing in CI**: Run tests in GitHub Actions before deployment
- [ ] **Terraform Apply**: Add workflow to apply Terraform changes (with approval gates)
- [ ] **Environment Promotion**: Set up dev → staging → prod promotion workflow
- [ ] **Rollback Strategy**: Implement rollback mechanism for failed deployments
- [ ] **Version Tagging**: Automatically tag releases and container images

### Infrastructure & Terraform

- [ ] **Backend Configuration**: Complete `terraform/backend.tf` with actual S3 bucket and DynamoDB table names
- [ ] **API Gateway Path**: Fix API Gateway resource path to include `/api` prefix (currently missing)
- [ ] **API Gateway Custom Domain**: Add custom domain configuration for API Gateway
- [ ] **SSL/TLS Certificates**: Configure ACM certificates for HTTPS endpoints
- [ ] **VPC Configuration**: Move Lambda to VPC if database access is required
- [ ] **Security Groups**: Review and tighten security group rules
- [ ] **IAM Roles**: Implement least-privilege IAM policies
- [ ] **CloudWatch Logs**: Configure log retention and log groups
- [ ] **CloudWatch Alarms**: Add alarms for Lambda errors, API Gateway 5xx errors, and ALB health
- [ ] **X-Ray Tracing**: Enable AWS X-Ray for distributed tracing
- [ ] **Cost Optimization**: Add cost tags and review resource sizing

### Security

- [ ] **API Authentication**: Implement API key or JWT authentication for REST API
- [ ] **gRPC Authentication**: Add TLS/mTLS for gRPC endpoints
- [ ] **CORS Configuration**: Configure CORS for API Gateway if needed
- [ ] **Input Validation**: Add comprehensive input validation and sanitization
- [ ] **Rate Limiting**: Implement rate limiting via API Gateway or WAF
- [ ] **Secrets Management**: Use AWS Secrets Manager for sensitive configuration
- [ ] **Security Headers**: Add security headers to API responses
- [ ] **Dependency Scanning**: Add Dependabot or Snyk for dependency vulnerability scanning

### Monitoring & Observability

- [ ] **Structured Logging**: Implement structured logging (JSON format) with correlation IDs
- [ ] **Metrics**: Add custom CloudWatch metrics for business logic
- [ ] **Dashboards**: Create CloudWatch dashboards for monitoring
- [ ] **Alerting**: Set up SNS topics and email/Slack notifications for critical alerts
- [ ] **Health Checks**: Implement comprehensive health check endpoint (`/health`, `/ready`, `/live`)
- [ ] **Distributed Tracing**: Integrate with AWS X-Ray for request tracing
- [ ] **Error Tracking**: Consider integrating with Sentry or similar for error tracking

### Documentation

- [ ] **API Documentation**: Add OpenAPI/Swagger documentation for REST API
- [ ] **gRPC Documentation**: Generate and publish gRPC API documentation
- [ ] **Architecture Diagrams**: Create detailed architecture diagrams (C4 model)
- [ ] **Runbooks**: Create operational runbooks for common tasks
- [ ] **Deployment Guide**: Document step-by-step deployment process
- [ ] **Troubleshooting Guide**: Expand troubleshooting section with common issues
- [ ] **Code Comments**: Add JavaDoc comments to all public methods and classes

### Application Features

- [ ] **Pagination**: Add pagination support for `ListTodos` endpoint
- [ ] **Filtering & Sorting**: Add query parameters for filtering and sorting todos
- [ ] **Search**: Implement full-text search capability
- [ ] **Bulk Operations**: Add bulk create/update/delete operations
- [ ] **Todo Categories/Tags**: Extend model to support categories or tags
- [ ] **Due Dates**: Add due date and reminder functionality
- [ ] **User Management**: Add multi-user support with user isolation
- [ ] **Audit Logging**: Log all create/update/delete operations

### Performance & Optimization

- [ ] **Caching**: Add Redis/ElastiCache for frequently accessed data
- [ ] **Lambda Cold Start**: Optimize Lambda cold start times (consider provisioned concurrency)
- [ ] **Connection Reuse**: Implement connection pooling for external services
- [ ] **Async Processing**: Add async processing for long-running operations
- [ ] **Compression**: Enable response compression for API Gateway
- [ ] **CDN**: Consider CloudFront for static assets (if any)

### gRPC Specific

- [ ] **gRPC over ALB**: Verify and test gRPC functionality through ALB
- [ ] **gRPC Reflection**: Enable gRPC reflection for easier testing
- [ ] **gRPC Interceptors**: Add logging and error handling interceptors
- [ ] **gRPC Health Service**: Implement gRPC health checking service
- [ ] **Streaming Support**: Consider adding streaming RPCs if needed

### Development Experience

- [ ] **Local DynamoDB**: Add LocalStack or DynamoDB Local for local development
- [ ] **Docker Compose**: Create docker-compose.yml for local development stack
- [ ] **Pre-commit Hooks**: Add pre-commit hooks for code formatting and linting
- [ ] **Code Formatting**: Configure Spotless or similar for consistent code formatting
- [ ] **Linting**: Add Checkstyle or PMD for code quality checks
- [ ] **GitHub Templates**: Add issue and PR templates
- [ ] **Development Scripts**: Create helper scripts for common development tasks

### Configuration & Environment

- [ ] **Environment Variables**: Document all required environment variables
- [ ] **Configuration Profiles**: Add Spring profiles for dev/staging/prod
- [ ] **Feature Flags**: Implement feature flags for gradual rollouts
- [ ] **Configuration Validation**: Add startup validation for required configuration

### Error Handling & Resilience

- [ ] **Error Responses**: Standardize error response format across REST and gRPC
- [ ] **Retry Logic**: Add retry logic for transient failures
- [ ] **Circuit Breaker**: Implement circuit breaker pattern for external dependencies
- [ ] **Graceful Shutdown**: Implement graceful shutdown handling
- [ ] **Dead Letter Queue**: Configure DLQ for failed Lambda invocations

### Compliance & Best Practices

- [ ] **Code Review Checklist**: Create code review checklist
- [ ] **Security Audit**: Perform security audit of the application
- [ ] **Compliance**: Ensure compliance with relevant standards (if applicable)
- [ ] **Backup Strategy**: Document backup and disaster recovery procedures
- [ ] **Resource Cleanup**: Add script to clean up AWS resources for testing

## License

This project is provided as-is for demonstration purposes.


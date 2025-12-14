# Multi-stage build for Lambda container image

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM public.ecr.aws/lambda/java:21

# Copy the JAR file
COPY --from=build /app/target/*.jar ${LAMBDA_TASK_ROOT}/lib/

# Set the handler class
ENV _HANDLER=com.example.todo.LambdaHandler::handleRequest

# Set the handler
CMD [ "com.example.todo.LambdaHandler::handleRequest" ]


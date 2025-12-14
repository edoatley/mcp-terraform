# Simple DevContainer Setup

This devcontainer uses the official Maven image with Java 21 - no custom Dockerfile needed!

## What's Included

- **Java 21** - Pre-installed in the Maven image
- **Maven 3.9** - Pre-installed in the Maven image
- **grpcurl** - Installed automatically via postCreateCommand
- **curl** - Installed automatically via postCreateCommand

## Usage

1. Open the project in VS Code
2. Click "Reopen in Container" when prompted
3. Wait for the container to start (first time will install grpcurl and build the project)
4. Start coding!

## Testing

Once the container is running:

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Test REST API (in another terminal)
curl http://localhost:8080/api/todos

# Test gRPC (in another terminal)
grpcurl -plaintext localhost:9090 list
```

## Notes

- Uses `root` user for simplicity (no user creation issues)
- All tools are installed via postCreateCommand
- The Maven image already has Java 21 and Maven 3.9



package com.example.todo.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for API tests that can run against local or deployed instances.
 * 
 * Configuration can be provided via:
 * - application-api-test.properties
 * - Environment variables (API_BASE_URL, GRPC_HOST, GRPC_PORT)
 * - System properties
 * 
 * This is a plain Java class (no Spring) for API tests that run against
 * an already-running application instance.
 */
public class ApiTestConfig {

    private static final String DEFAULT_API_BASE_URL = "http://localhost:8080";
    private static final String DEFAULT_GRPC_HOST = "localhost";
    private static final int DEFAULT_GRPC_PORT = 9090;

    private final String apiBaseUrl;
    private final String grpcHost;
    private final int grpcPort;

    public ApiTestConfig() {
        Properties props = loadProperties();
        
        // Read from system properties, then environment variables, then properties file, then defaults
        this.apiBaseUrl = getProperty(props, "api.base.url", "API_BASE_URL", DEFAULT_API_BASE_URL);
        this.grpcHost = getProperty(props, "grpc.host", "GRPC_HOST", DEFAULT_GRPC_HOST);
        this.grpcPort = Integer.parseInt(getProperty(props, "grpc.port", "GRPC_PORT", String.valueOf(DEFAULT_GRPC_PORT)));
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application-api-test.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // Properties file is optional
        }
        return props;
    }

    private String getProperty(Properties props, String propKey, String envKey, String defaultValue) {
        // System properties take precedence
        String value = System.getProperty(propKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Then environment variables
        value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Then properties file
        value = props.getProperty(propKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Finally default
        return defaultValue;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getGrpcHost() {
        return grpcHost;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public String getTodosEndpoint() {
        return apiBaseUrl + "/api/todos";
    }
}

plugins {
    java
    id("org.springframework.boot") version "3.3.6"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.google.protobuf") version "0.9.4"
    kotlin("jvm") version "1.9.24"
    jacoco
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.60.0"
val protobufVersion = "3.25.1"
val awsSdkVersion = "2.21.29"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // AWS Lambda
    implementation("com.amazonaws.serverless:aws-serverless-java-container-springboot3:2.1.5")
    
    // gRPC
    implementation("net.devh:grpc-spring-boot-starter:3.1.0.RELEASE")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-core:$grpcVersion")
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    
    // Protocol Buffers
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    
    // javax.annotation for Java 9+
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // AWS SDK v2 DynamoDB
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:dynamodb-enhanced")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Create apiTest source set
sourceSets {
    create("apiTest") {
        java.srcDir("src/apiTest/java")
        resources.srcDir("src/apiTest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

// Configure processApiTestResources to handle duplicates
tasks.named<ProcessResources>("processApiTestResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

configurations {
    val apiTestImplementation by getting {
        extendsFrom(configurations["testImplementation"])
    }
    val apiTestRuntimeOnly by getting {
        extendsFrom(configurations["testRuntimeOnly"])
    }
}

// Add dependencies for apiTest source set
dependencies {
    // API Testing - separate source set
    add("apiTestImplementation", "org.springframework.boot:spring-boot-starter-test")
    add("apiTestImplementation", "io.rest-assured:rest-assured")
    add("apiTestImplementation", "io.rest-assured:json-path")
    add("apiTestImplementation", sourceSets["main"].output)
    add("apiTestImplementation", sourceSets["test"].output)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Configure regular test task (excludes API tests)
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    exclude("**/api/**")
}

// Create apiTest task
val apiTest = tasks.register<Test>("apiTest") {
    description = "Runs API tests against a running application instance"
    group = "verification"
    
    testClassesDirs = sourceSets["apiTest"].output.classesDirs
    classpath = sourceSets["apiTest"].runtimeClasspath
    
    useJUnitPlatform()
    
    // Don't include API tests in coverage reports
    shouldRunAfter(tasks.test)

    // Enhanced test logging
    testLogging {
        events("passed", "skipped", "failed", "standard_out", "standard_error")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
    
    // Always show test output
    outputs.upToDateWhen { false }
}

tasks.bootJar {
    enabled = true
}

val jacocoExcludes = listOf(
    "com/example/todo/TodoApplication.class",
    "com/example/todo/LambdaHandler.class",
    "com/example/todo/model/**",
    "com/example/todo/proto/**"
)

// JaCoCo Configuration
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true) // Enable CSV for easier parsing
    }
    finalizedBy(tasks.jacocoTestCoverageVerification)
    
    // Exclude generated classes from coverage report
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    jacocoExcludes
                )
            }
        })
    )
    
    // Print coverage summary to console
    doLast {
        val xmlReport = reports.xml.outputLocation.get().asFile
        if (xmlReport.exists()) {
            try {
                val xmlContent = xmlReport.readText()
                
                // Extract coverage metrics from XML using regex (simple approach)
                val instructionRegex = """type="INSTRUCTION".*?missed="(\d+)".*?covered="(\d+)"""".toRegex()
                val branchRegex = """type="BRANCH".*?missed="(\d+)".*?covered="(\d+)"""".toRegex()
                val lineRegex = """type="LINE".*?missed="(\d+)".*?covered="(\d+)"""".toRegex()
                
                var totalInstructionsMissed = 0
                var totalInstructionsCovered = 0
                var totalBranchesMissed = 0
                var totalBranchesCovered = 0
                var totalLinesMissed = 0
                var totalLinesCovered = 0
                
                instructionRegex.findAll(xmlContent).forEach { match ->
                    totalInstructionsMissed += match.groupValues[1].toIntOrNull() ?: 0
                    totalInstructionsCovered += match.groupValues[2].toIntOrNull() ?: 0
                }
                
                branchRegex.findAll(xmlContent).forEach { match ->
                    totalBranchesMissed += match.groupValues[1].toIntOrNull() ?: 0
                    totalBranchesCovered += match.groupValues[2].toIntOrNull() ?: 0
                }
                
                lineRegex.findAll(xmlContent).forEach { match ->
                    totalLinesMissed += match.groupValues[1].toIntOrNull() ?: 0
                    totalLinesCovered += match.groupValues[2].toIntOrNull() ?: 0
                }
                
                val totalInstructions = totalInstructionsMissed + totalInstructionsCovered
                val totalBranches = totalBranchesMissed + totalBranchesCovered
                val totalLines = totalLinesMissed + totalLinesCovered
                
                val instructionCoverage = if (totalInstructions > 0) {
                    String.format("%.2f", totalInstructionsCovered.toDouble() / totalInstructions * 100)
                } else "0.00"
                
                val branchCoverage = if (totalBranches > 0) {
                    String.format("%.2f", totalBranchesCovered.toDouble() / totalBranches * 100)
                } else "0.00"
                
                val lineCoverage = if (totalLines > 0) {
                    String.format("%.2f", totalLinesCovered.toDouble() / totalLines * 100)
                } else "0.00"
                
                println("\n" + "=".repeat(70))
                println("JaCoCo Test Coverage Summary")
                println("=".repeat(70))
                println("Instructions: $totalInstructionsCovered/$totalInstructions ($instructionCoverage%)")
                println("Branches:     $totalBranchesCovered/$totalBranches ($branchCoverage%)")
                println("Lines:        $totalLinesCovered/$totalLines ($lineCoverage%)")
                println("=".repeat(70))
                println("HTML Report: ${reports.html.outputLocation.get().asFile.absolutePath}/index.html")
                println("XML Report:  ${reports.xml.outputLocation.get().asFile.absolutePath}")
                println("=".repeat(70) + "\n")
            } catch (e: Exception) {
                // Silently fail if parsing doesn't work
            }
        }
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    
    // Exclude generated classes from coverage verification
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    jacocoExcludes
                )
            }
        })
    )
    
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}


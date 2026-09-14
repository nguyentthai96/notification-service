plugins {
    id("ntt.spring-app-conventions")
    alias(libs.plugins.kotlin.jpa)
    id("com.google.protobuf") version "0.9.4"
}

extra["springCloudVersion"] = libs.versions.spring.cloud.get()

dependencies {
//  - BASE-CORE STARTERS (provides base-core, base-model, common-log transitively)
    implementation(platform("com.ntt:platform:0.0.1-SNAPSHOT"))
    implementation("com.ntt:base-web-starter")
    implementation("com.ntt:base-data-starter")
    implementation("com.ntt:common-log")
//  - MAIN
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-webflux")    // WebClient for OTT APIs
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    // Resilience4j — circuit breaker per notification channel
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

//  - PHASE 2: Channel providers
    implementation("com.twilio.sdk:twilio:10.6.3")                              // SMS via Twilio
    implementation("com.google.firebase:firebase-admin:9.3.0")                  // Push via FCM

//  - PHASE 2: Transport (optional — plug-and-play)
    implementation("org.springframework.kafka:spring-kafka")                    // Kafka consumer/producer
    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0")            // gRPC server
    implementation("io.grpc:grpc-protobuf:1.68.0")                              // Protobuf serialization
    implementation("io.grpc:grpc-stub:1.68.0")
    implementation("com.google.protobuf:protobuf-java:4.28.2")

//  - DEVELOPMENT
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:postgresql")
//  - TESTING
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.4")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.4")
    testImplementation("com.ntt:base-testing-starter")
    testRuntimeOnly("com.h2database:h2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.2"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.68.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

// Disable GraalVM AOT processing — runs as standard JVM app.
tasks.named("processAot") { enabled = false }
tasks.named("processTestAot") { enabled = false }

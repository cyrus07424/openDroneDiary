
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "2.2.21"
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation("io.ktor:ktor-server-html-builder:3.3.2")
    implementation("io.ktor:ktor-server-sessions:3.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.2")
    implementation("io.ktor:ktor-server-status-pages:3.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0")
    
    // JavaMail for SMTP email
    implementation("javax.mail:javax.mail-api:1.6.2")
    implementation("com.sun.mail:javax.mail:1.6.2")
    
    // Database dependencies
    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.50.1")
    implementation("org.postgresql:postgresql:42.7.8")
    implementation("com.zaxxer:HikariCP:7.0.2")
    
    // H2 database for local development/testing
    implementation("com.h2database:h2:2.3.232")
    
    // bcrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")
    
    // zxcvbn for password strength validation
    implementation("com.nulab-inc:zxcvbn:1.9.0")
    
    // HTTP client for Slack notifications
    implementation("io.ktor:ktor-client-core:3.3.2")
    implementation("io.ktor:ktor-client-cio:3.3.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.2")
    
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

tasks {
    create("stage").dependsOn("installDist")
}
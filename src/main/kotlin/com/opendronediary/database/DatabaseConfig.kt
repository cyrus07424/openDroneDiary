package com.opendronediary.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseConfig {
    fun initDatabase() {
        val databaseUrl = System.getenv("DATABASE_URL")
        
        val database = if (databaseUrl != null) {
            // Parse Heroku DATABASE_URL format: postgres://user:password@host:port/database
            val uri = URI(databaseUrl)
            val host = uri.host
            val port = uri.port
            val database = uri.path.substring(1) // Remove leading slash
            val userInfo = uri.userInfo.split(":")
            val username = userInfo[0]
            val password = userInfo[1]
            
            val hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://$host:$port/$database"
                this.username = username
                this.password = password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 10
                minimumIdle = 2
                connectionTimeout = 30000
                idleTimeout = 600000
                maxLifetime = 1800000
            }
            
            val dataSource = HikariDataSource(hikariConfig)
            Database.connect(dataSource)
        } else {
            // For local development or testing, use an in-memory H2 database
            // This fallback ensures the app can run even without PostgreSQL
            Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
                driver = "org.h2.Driver"
            )
        }
        
        // Create tables
        transaction {
            SchemaUtils.create(
                Users,
                Pilots,
                FlightLogs,
                DailyInspectionRecords,
                MaintenanceInspectionRecords,
                PasswordResetTokens
            )
        }
    }
}
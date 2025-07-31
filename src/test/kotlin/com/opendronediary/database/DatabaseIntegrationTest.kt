package com.opendronediary.database

import com.opendronediary.model.User
import com.opendronediary.repository.UserRepository
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DatabaseIntegrationTest {
    
    @Test
    fun testDatabaseInitializationAndBasicOperations() {
        // Initialize database (should use H2 fallback since DATABASE_URL is not set)
        DatabaseConfig.initDatabase()
        
        // Test basic user operations
        val userRepository = UserRepository()
        
        // Add a user
        val user = User(0, "testuser", "hashedpassword", "testuser@example.com")
        val addedUser = userRepository.add(user)
        
        assertNotNull(addedUser)
        assertEquals("testuser", addedUser.username)
        assertEquals("hashedpassword", addedUser.passwordHash)
        
        // Find user by username
        val foundUser = userRepository.findByUsername("testuser")
        assertNotNull(foundUser)
        assertEquals("testuser", foundUser.username)
        
        // Get user by ID
        val userById = userRepository.getById(addedUser.id)
        assertNotNull(userById)
        assertEquals("testuser", userById.username)
    }
}
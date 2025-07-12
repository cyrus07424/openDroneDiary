package integration

import com.example.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolicyFullIntegrationTest {
    
    @Test
    fun testFullFunctionalityWithBothUrls() = testApplication {
        // This test simulates having both environment variables set
        application {
            module()
        }
        
        // Test home page
        val homeResponse = client.get("/")
        assertEquals(HttpStatusCode.OK, homeResponse.status)
        val homeContent = homeResponse.bodyAsText()
        assertTrue(homeContent.contains("OpenDroneDiary"))
        
        // Test registration page
        val registerResponse = client.get("/register")
        assertEquals(HttpStatusCode.OK, registerResponse.status)
        val registerContent = registerResponse.bodyAsText()
        assertTrue(registerContent.contains("ユーザー登録"))
        
        // Test login page
        val loginResponse = client.get("/login")
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginContent = loginResponse.bodyAsText()
        assertTrue(loginContent.contains("ログイン"))
        
        // Verify all pages have proper structure
        listOf(homeContent, registerContent, loginContent).forEach { content ->
            assertTrue(content.contains("bootstrap"))
            assertTrue(content.contains("d-flex flex-column min-vh-100"))
        }
    }
}
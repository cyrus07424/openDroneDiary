package integration

import com.example.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyIntegrationTest {
    
    @Test
    fun testRegistrationPageWithoutTermsUrl() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/register")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val htmlContent = response.bodyAsText()
        // Should not contain terms checkbox when environment variable is not set
        assertFalse(htmlContent.contains("agreeToTerms"))
        assertFalse(htmlContent.contains("利用規約に同意します"))
    }
    
    @Test
    fun testFooterWithoutPolicyUrls() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        
        val htmlContent = response.bodyAsText()
        // Should not contain footer when environment variables are not set
        assertFalse(htmlContent.contains("<footer"))
        assertFalse(htmlContent.contains("利用規約"))
        assertFalse(htmlContent.contains("プライバシーポリシー"))
        assertFalse(htmlContent.contains("LPトップページ"))
    }
    
    @Test
    fun testBasicPageStructure() = testApplication {
        application {
            module()
        }
        
        val pages = listOf("/", "/login", "/register")
        
        pages.forEach { page ->
            val response = client.get(page)
            assertEquals(HttpStatusCode.OK, response.status)
            
            val htmlContent = response.bodyAsText()
            assertTrue(htmlContent.contains("<!DOCTYPE html>"))
            assertTrue(htmlContent.contains("<head>"))
            assertTrue(htmlContent.contains("<body"))
            assertTrue(htmlContent.contains("bootstrap"))
        }
    }
}
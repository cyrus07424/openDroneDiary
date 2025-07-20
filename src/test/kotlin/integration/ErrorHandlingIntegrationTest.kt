package integration

import io.ktor.server.testing.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import com.example.module

class ErrorHandlingIntegrationTest {

    @Test
    fun testNotFoundPageReturns404WithErrorPage() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/nonexistent-page")
        
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.headers[HttpHeaders.ContentType]?.contains("text/html") ?: false)
    }

    @Test 
    fun testMainPageReturnsSuccessfully() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/")
        
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers[HttpHeaders.ContentType]?.contains("text/html") ?: false)
    }
}
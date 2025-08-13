package integration

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import com.example.module

class FormModalIntegrationTest {
    
    @Test
    fun `login page contains modal dialog with JavaScript`() = testApplication {
        application {
            module()
        }
        
        client.get("/login").apply {
            assertEquals(HttpStatusCode.OK, status)
            val content = bodyAsText()
            
            // Check that modal dialog HTML is present
            assertTrue(content.contains("id=\"loadingModal\""), "Modal dialog should be present")
            assertTrue(content.contains("modal fade"), "Bootstrap modal classes should be present")
            assertTrue(content.contains("spinner-border"), "Loading spinner should be present")
            assertTrue(content.contains("id=\"loadingModalMessage\""), "Modal message element should be present")
            
            // Check that JavaScript for form handling is present
            assertTrue(content.contains("FORM_MESSAGES"), "Form messages object should be present")
            assertTrue(content.contains("addEventListener"), "Event listeners should be added")
            assertTrue(content.contains("bootstrap.Modal"), "Bootstrap modal JavaScript should be used")
            assertTrue(content.contains("disabled = true"), "Submit button should be disabled")
            
            // Check for specific form messages
            assertTrue(content.contains("ログイン中です..."), "Login message should be present")
            assertTrue(content.contains("登録中です..."), "Registration message should be present")
            assertTrue(content.contains("削除中です..."), "Delete message should be present")
            assertTrue(content.contains("更新中です..."), "Update message should be present")
        }
    }
    
    @Test
    fun `register page contains modal dialog`() = testApplication {
        application {
            module()
        }
        
        client.get("/register").apply {
            assertEquals(HttpStatusCode.OK, status)
            val content = bodyAsText()
            
            // Check that modal dialog is present
            assertTrue(content.contains("id=\"loadingModal\""), "Modal dialog should be present")
            assertTrue(content.contains("処理中です..."), "Default processing message should be present")
        }
    }
}
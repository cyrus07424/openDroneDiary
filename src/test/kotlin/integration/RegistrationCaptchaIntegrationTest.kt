package integration

import com.example.module
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RegistrationCaptchaIntegrationTest {

    @Test
    fun `register captcha image endpoint returns png`() = testApplication {
        application {
            module()
        }

        val registerPage = client.get("/register")
        assertEquals(HttpStatusCode.OK, registerPage.status)

        val challengeId = extractChallengeId(registerPage.bodyAsText())
        assertNotNull(challengeId)

        val captchaResponse = client.get("/register/captcha/$challengeId")
        assertEquals(HttpStatusCode.OK, captchaResponse.status)
        assertEquals("image/png", captchaResponse.headers["Content-Type"])
    }

    @Test
    fun `register rejects invalid captcha answer`() = testApplication {
        application {
            module()
        }

        val registerPage = client.get("/register")
        assertEquals(HttpStatusCode.OK, registerPage.status)

        val challengeId = extractChallengeId(registerPage.bodyAsText())
        assertNotNull(challengeId)

        val response = client.post("/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("username", "captcha_test_${System.currentTimeMillis()}")
                        append("email", "captcha_test_${System.currentTimeMillis()}@example.com")
                        append("password", "MyCaptcha!Str0ngP@ssword")
                        append("captchaChallengeId", challengeId)
                        append("captchaAnswer", "wrong")
                    }
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    private fun extractChallengeId(content: String): String? {
        val match = Regex("""name="captchaChallengeId"[^>]*value="([^"]+)"""").find(content)
        return match?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }
}

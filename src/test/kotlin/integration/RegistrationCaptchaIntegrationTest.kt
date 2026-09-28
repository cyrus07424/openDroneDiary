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
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import routing.RegisterFormState
import routing.registerPage
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

        val username = "captcha_test_${System.currentTimeMillis()}"
        val email = "$username@example.com"
        val response = client.post("/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("username", username)
                        append("email", email)
                        append("password", "MyCaptcha!Str0ngP@ssword")
                        append("captchaChallengeId", challengeId)
                        append("captchaAnswer", "wrong")
                    }
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        kotlin.test.assertTrue(body.contains("画像認証に失敗しました"))
        kotlin.test.assertFalse(body.contains("リクエストが正しくありません"))
        // エラー時もフォームに戻り、入力値が保持されていること
        kotlin.test.assertTrue(body.contains("""name="captchaAnswer""""), "フォームが再表示されること")
        kotlin.test.assertTrue(body.contains("is-invalid"), "該当項目にエラー表示が付くこと")
        kotlin.test.assertTrue(body.contains(username), "ユーザー名が保持されること")
        kotlin.test.assertTrue(body.contains(email), "メールアドレスが保持されること")
        val newChallengeId = extractChallengeId(body)
        assertNotNull(newChallengeId)
        kotlin.test.assertNotEquals(challengeId, newChallengeId, "画像認証は作り直されること")
    }

    @Test
    fun `register shows inline errors for missing fields`() = testApplication {
        application {
            module()
        }

        val registerPage = client.get("/register")
        val challengeId = extractChallengeId(registerPage.bodyAsText())
        assertNotNull(challengeId)

        val response = client.post("/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("username", "")
                        append("email", "not-an-email")
                        append("password", "")
                        append("captchaChallengeId", challengeId)
                        append("captchaAnswer", "whatever")
                    }
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        kotlin.test.assertTrue(body.contains("ユーザー名を入力してください"))
        kotlin.test.assertTrue(body.contains("メールアドレスの形式が正しくありません"))
        kotlin.test.assertTrue(body.contains("パスワードを入力してください"))
        kotlin.test.assertTrue(body.contains("not-an-email"), "入力値が保持されること")
    }

    @Test
    fun `register page renders inline error for weak password`() {
        val html = buildString {
            appendHTML().html {
                registerPage(
                    "challenge-id",
                    RegisterFormState(
                        username = "weak_user",
                        email = "weak_user@example.com",
                        fieldErrors = mapOf("password" to "よく使われるパスワードです 推奨：単語を増やしてください"),
                        summaryMessage = "パスワードの強度が足りません。"
                    )
                )
            }
        }

        kotlin.test.assertTrue(html.contains("パスワードの強度が足りません"))
        kotlin.test.assertTrue(html.contains("よく使われるパスワードです"))
        kotlin.test.assertTrue(html.contains("is-invalid"))
        kotlin.test.assertTrue(html.contains("weak_user@example.com"), "入力値が保持されること")
    }

    private fun extractChallengeId(content: String): String? {
        val match = Regex("""name="captchaChallengeId"[^>]*value="([^"]+)"""").find(content)
        return match?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }
}

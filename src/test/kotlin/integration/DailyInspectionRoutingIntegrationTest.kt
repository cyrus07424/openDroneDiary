package integration

import com.example.module
import com.opendronediary.database.DatabaseConfig
import com.opendronediary.model.DailyInspectionRecord
import com.opendronediary.model.User
import com.opendronediary.model.UserSession
import com.opendronediary.repository.DailyInspectionRecordRepository
import com.opendronediary.repository.UserRepository
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.testApplication
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyInspectionRoutingIntegrationTest {
    init {
        DatabaseConfig.initDatabase()
    }

    @Test
    fun `daily inspection list renders remarks separately when checklist values exist`() = testApplication {
        var testUser = User(id = 0, username = "", passwordHash = "", email = "")
        application {
            module()
            routing {
                get("/test-session") {
                    call.sessions.set(UserSession(testUser.id, testUser.username))
                    call.respondText("ok")
                }
            }
        }

        val stamp = System.nanoTime()
        val user = UserRepository().add(
            User(
                id = 0,
                username = "daily_inspection_$stamp",
                passwordHash = "hashed",
                email = "daily_inspection_$stamp@example.com"
            )
        )
        testUser = user
        DailyInspectionRecordRepository().add(
            DailyInspectionRecord(
                id = 0,
                inspectionDate = "2026-11-26",
                location = "点検場所",
                inspectorName = "点検者",
                inspectionResult = "プロペラに傷あり",
                userId = user.id,
                airframeResult = "normal"
            )
        )

        val sessionResponse = client.get("/test-session")
        val sessionCookie = sessionResponse.headers[HttpHeaders.SetCookie]
            ?.substringBefore(";")
            ?: error("セッションCookieが設定されませんでした")
        val response = client.get("/dailyinspections/ui") {
            headers.append(HttpHeaders.Cookie, sessionCookie)
        }
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("<th>特記事項</th>"))
        assertTrue(html.contains("<td>プロペラに傷あり</td>"))
        assertTrue(html.contains("異常なし"))
    }
}

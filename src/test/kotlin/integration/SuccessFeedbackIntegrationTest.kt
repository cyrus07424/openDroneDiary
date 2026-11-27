package integration

import com.example.module
import com.opendronediary.database.DatabaseConfig
import com.opendronediary.model.User
import com.opendronediary.model.UserSession
import com.opendronediary.repository.AircraftRepository
import com.opendronediary.repository.DailyInspectionRecordRepository
import com.opendronediary.repository.FlightLogRepository
import com.opendronediary.repository.MaintenanceInspectionRecordRepository
import com.opendronediary.repository.PilotRepository
import com.opendronediary.repository.UserRepository
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SuccessFeedbackIntegrationTest {
    init {
        DatabaseConfig.initDatabase()
    }

    @Test
    fun `CRUD redirects display success messages on each list`() = testApplication {
        val stamp = System.nanoTime()
        val user = UserRepository().add(
            User(0, "feedback_$stamp", "hashed", "feedback_$stamp@example.com")
        )
        application {
            module()
            routing {
                get("/test-feedback-session") {
                    call.sessions.set(UserSession(user.id, user.username))
                    call.respondText("ok")
                }
            }
        }
        val client = createClient { followRedirects = false }
        val cookie = client.get("/test-feedback-session").headers[HttpHeaders.SetCookie]
            ?.substringBefore(";") ?: error("セッションCookieが設定されませんでした")

        data class Case(val path: String, val values: Map<String, String>, val id: () -> Int)
        val cases = listOf(
            Case("/aircraft/ui", mapOf("registrationSymbol" to "JU$stamp")) {
                transaction { AircraftRepository().getAllByUserId(user.id).single().id }
            },
            Case("/pilots/ui", mapOf("name" to "操縦者")) {
                transaction { PilotRepository().getAllByUserId(user.id).single().id }
            },
            Case("/dailyinspections/ui", mapOf("inspectionDate" to "2026-11-26", "location" to "場所", "inspectorName" to "点検者")) {
                DailyInspectionRecordRepository().getAllByUserId(user.id).single().id
            },
            Case("/flightlogs/ui", mapOf("flightDate" to "2026-11-26", "pilotNameText" to "操縦者")) {
                FlightLogRepository().getAllByUserId(user.id).single().id
            },
            Case("/maintenanceinspections/ui", mapOf("inspectionDate" to "2026-11-26", "location" to "場所", "inspectorName" to "点検者", "contentAndReason" to "整備")) {
                MaintenanceInspectionRecordRepository().getAllByUserId(user.id).single().id
            }
        )

        for (case in cases) {
            val unauthorized = client.get("${case.path}?success=saved")
            assertEquals(HttpStatusCode.Found, unauthorized.status)
            assertEquals("/login", unauthorized.headers[HttpHeaders.Location])

            val normal = client.get(case.path) { headers.append(HttpHeaders.Cookie, cookie) }
            assertEquals(HttpStatusCode.OK, normal.status)
            assertFalse(normal.bodyAsText().contains("alert alert-success"))
            val invalid = client.get("${case.path}?success=%3Cscript%3E") {
                headers.append(HttpHeaders.Cookie, cookie)
            }
            assertFalse(invalid.bodyAsText().contains("alert alert-success"))

            suspend fun submit(path: String, values: Map<String, String>) = client.submitForm(
                url = path,
                formParameters = parameters { values.forEach { (key, value) -> append(key, value) } }
            ) {
                headers.append(HttpHeaders.Cookie, cookie)
            }

            suspend fun assertRedirectAndMessage(response: io.ktor.client.statement.HttpResponse, result: String, message: String) {
                assertEquals(HttpStatusCode.Found, response.status)
                assertEquals("${case.path}?success=$result", response.headers[HttpHeaders.Location])
                val list = client.get(response.headers[HttpHeaders.Location]!!) {
                    headers.append(HttpHeaders.Cookie, cookie)
                }
                assertEquals(HttpStatusCode.OK, list.status)
                assertTrue(list.bodyAsText().contains("alert alert-success"))
                assertTrue(list.bodyAsText().contains(message))
            }

            assertRedirectAndMessage(submit(case.path, case.values), "saved", "保存しました")
            val id = case.id()
            assertRedirectAndMessage(submit("${case.path}/$id", case.values + ("_method" to "put")), "saved", "保存しました")
            assertRedirectAndMessage(submit("${case.path}/$id", mapOf("_method" to "delete")), "deleted", "削除しました")
            val missing = submit("${case.path}/$id", mapOf("_method" to "delete"))
            assertEquals(HttpStatusCode.NotFound, missing.status)
            assertEquals(null, missing.headers[HttpHeaders.Location])
        }
    }
}

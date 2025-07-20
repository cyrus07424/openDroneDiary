package utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*
import utils.GTMHelper.addGTMBodyScript
import utils.GTMHelper.addGTMHeadScript
import utils.PolicyHelper.addFooter

object ErrorPageHelper {
    
    suspend fun respondWithErrorPage(call: ApplicationCall, statusCode: HttpStatusCode, errorTitle: String, errorMessage: String) {
        call.respondHtml(statusCode) {
            head {
                title { +errorTitle }
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css")
                script(src = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js") { }
                addGTMHeadScript()
            }
            body(classes = "d-flex flex-column min-vh-100") {
                addGTMBodyScript()
                div(classes = "container mt-5") {
                    div(classes = "row justify-content-center") {
                        div(classes = "col-md-8") {
                            div(classes = "card") {
                                div(classes = "card-header") {
                                    h1(classes = "card-title mb-0") { +errorTitle }
                                }
                                div(classes = "card-body") {
                                    div(classes = "alert alert-danger") {
                                        +errorMessage
                                    }
                                    div(classes = "d-grid gap-2") {
                                        a(href = "/", classes = "btn btn-primary") { +"トップページに戻る" }
                                    }
                                }
                            }
                        }
                    }
                }
                addFooter()
            }
        }
    }
    
    suspend fun respondWithSystemError(call: ApplicationCall) {
        respondWithErrorPage(
            call,
            HttpStatusCode.InternalServerError,
            "システムエラー",
            "申し訳ございません。システムエラーが発生しました。しばらく時間をおいてから再度お試しください。"
        )
    }
}
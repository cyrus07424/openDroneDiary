package routing

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.UserSession
import com.opendronediary.service.UserService
import com.opendronediary.service.EmailService
import com.opendronediary.service.SlackService
import io.ktor.server.html.respondHtml
import kotlinx.html.*
import utils.GTMHelper.addGTMHeadScript
import utils.GTMHelper.addGTMBodyScript
import utils.PolicyHelper.addFooter
import utils.PolicyHelper.isTermsOfServiceEnabled
import utils.PolicyHelper.getTermsOfServiceUrl
import utils.RequestContextHelper

// Helper function to create Bootstrap head with CDN links
fun HEAD.bootstrapHead(pageTitle: String) {
    title { +pageTitle }
    meta(charset = "utf-8")
    meta(name = "viewport", content = "width=device-width, initial-scale=1")
    link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css")
    script(src = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js") { }
    addGTMHeadScript()
}

fun Route.configureTopAndAuthRouting(userService: UserService, emailService: EmailService, slackService: SlackService) {
    get("/") {
        val session = call.sessions.get<UserSession>()
        call.respondHtml {
            head { bootstrapHead("トップ") }
            body(classes = "d-flex flex-column min-vh-100") {
                addGTMBodyScript()
                div(classes = "container mt-5") {
                    div(classes = "row justify-content-center") {
                        div(classes = "col-md-8") {
                            div(classes = "card") {
                                div(classes = "card-header") {
                                    h1(classes = "card-title mb-0") { +"🛩️ OpenDroneDiary 🚁" }
                                }
                                div(classes = "card-body") {
                                    if (session != null) {
                                        div(classes = "alert alert-success") { +"ログイン中: ${session.username}" }
                                        div(classes = "d-grid gap-2") {
                                            a(href = "/flightlogs/ui", classes = "btn btn-primary") { +"飛行記録一覧へ" }
                                            a(href = "/dailyinspections/ui", classes = "btn btn-primary") { +"日常点検記録一覧へ" }
                                            a(href = "/maintenanceinspections/ui", classes = "btn btn-primary") { +"点検整備記録一覧へ" }
                                            a(href = "/logout", classes = "btn btn-outline-secondary") { +"ログアウト" }
                                        }
                                    } else {
                                        p(classes = "card-text") { +"ドローンの飛行日誌を管理するためのオープンソースのツールです。" }
                                        div(classes = "d-grid gap-2") {
                                            a(href = "/login", classes = "btn btn-primary") { +"ログイン" }
                                            a(href = "/register", classes = "btn btn-outline-primary") { +"ユーザー登録" }
                                        }
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
    
    // User authentication routes
    get("/login") {
        call.respondHtml {
            head { bootstrapHead("ログイン") }
            body(classes = "d-flex flex-column min-vh-100") {
                addGTMBodyScript()
                div(classes = "container mt-5") {
                    div(classes = "row justify-content-center") {
                        div(classes = "col-md-6") {
                            div(classes = "card") {
                                div(classes = "card-header") {
                                    h1(classes = "card-title mb-0") { +"ログイン" }
                                }
                                div(classes = "card-body") {
                                    form(action = "/login", method = FormMethod.post) {
                                        div(classes = "mb-3") {
                                            label(classes = "form-label") { +"ユーザー名" }
                                            textInput(classes = "form-control") { 
                                                name = "username"
                                                placeholder = "ユーザー名を入力してください"
                                                required = true
                                            }
                                        }
                                        div(classes = "mb-3") {
                                            label(classes = "form-label") { +"パスワード" }
                                            passwordInput(classes = "form-control") { 
                                                name = "password"
                                                placeholder = "パスワードを入力してください"
                                                required = true
                                            }
                                        }
                                        div(classes = "d-grid") {
                                            submitInput(classes = "btn btn-primary") { value = "ログイン" }
                                        }
                                    }
                                    hr()
                                    div(classes = "text-center") {
                                        a(href = "/register", classes = "btn btn-link") { +"ユーザー登録はこちら" }
                                        br()
                                        a(href = "/forgot-password", classes = "btn btn-link text-muted") { +"パスワードを忘れた場合" }
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
    
    post("/login") {
        val params = call.receiveParameters()
        val username = params["username"] ?: ""
        val password = params["password"] ?: ""
        
        val user = userService.login(username, password)
        if (user != null) {
            call.sessions.set(UserSession(user.id, user.username))
            
            // Send Slack notification for successful login
            try {
                val userAgent = RequestContextHelper.extractUserAgent(call)
                val ipAddress = RequestContextHelper.extractIpAddress(call)
                slackService.sendNotification(
                    action = "ユーザーログイン",
                    username = user.username,
                    userAgent = userAgent,
                    ipAddress = ipAddress
                )
            } catch (e: Exception) {
                // Log error but don't fail the login process
                call.application.log.error("Failed to send Slack notification for login", e)
            }
            
            call.respondRedirect("/")
        } else {
            call.respondHtml(HttpStatusCode.Unauthorized) {
                head { bootstrapHead("ログインエラー") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"ログインエラー" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-danger") {
                                            +"ユーザー名またはパスワードが間違っています。"
                                        }
                                        a(href = "/login", classes = "btn btn-primary") { +"戻る" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
        }
    }
    
    get("/register") {
        call.respondHtml {
            head { bootstrapHead("ユーザー登録") }
            body(classes = "d-flex flex-column min-vh-100") {
                addGTMBodyScript()
                div(classes = "container mt-5") {
                    div(classes = "row justify-content-center") {
                        div(classes = "col-md-6") {
                            div(classes = "card") {
                                div(classes = "card-header") {
                                    h1(classes = "card-title mb-0") { +"ユーザー登録" }
                                }
                                div(classes = "card-body") {
                                    form(action = "/register", method = FormMethod.post) {
                                        div(classes = "mb-3") {
                                            label(classes = "form-label") { +"ユーザー名" }
                                            textInput(classes = "form-control") { 
                                                name = "username"
                                                placeholder = "ユーザー名を入力してください"
                                                required = true
                                            }
                                        }
                                        div(classes = "mb-3") {
                                            label(classes = "form-label") { +"メールアドレス" }
                                            emailInput(classes = "form-control") { 
                                                name = "email"
                                                placeholder = "メールアドレスを入力してください"
                                                required = true
                                            }
                                        }
                                        div(classes = "mb-3") {
                                            label(classes = "form-label") { +"パスワード" }
                                            passwordInput(classes = "form-control") { 
                                                name = "password"
                                                placeholder = "パスワードを入力してください"
                                                required = true
                                            }
                                        }
                                        
                                        // Terms of Service checkbox - only show if URL is configured
                                        if (isTermsOfServiceEnabled()) {
                                            div(classes = "mb-3") {
                                                div(classes = "form-check") {
                                                    checkBoxInput(classes = "form-check-input") {
                                                        name = "agreeToTerms"
                                                        id = "agreeToTerms"
                                                        required = true
                                                    }
                                                    label(classes = "form-check-label") {
                                                        htmlFor = "agreeToTerms"
                                                        unsafe {
                                                            raw("""<a href="${getTermsOfServiceUrl()}" target="_blank" class="text-decoration-none">利用規約</a>に同意します""")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        div(classes = "d-grid") {
                                            submitInput(classes = "btn btn-success") { value = "登録" }
                                        }
                                    }
                                    hr()
                                    div(classes = "text-center") {
                                        a(href = "/login", classes = "btn btn-link") { +"ログインはこちら" }
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
    
    post("/register") {
        val params = call.receiveParameters()
        val username = params["username"] ?: ""
        val password = params["password"] ?: ""
        val email = params["email"] ?: ""
        val agreeToTerms = params["agreeToTerms"] ?: ""
        
        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            call.respondHtml(HttpStatusCode.BadRequest) {
                head { bootstrapHead("登録エラー") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"入力エラー" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-danger") {
                                            +"ユーザー名、メールアドレス、パスワードは必須です。"
                                        }
                                        a(href = "/register", classes = "btn btn-primary") { +"戻る" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
            return@post
        }
        
        // Check terms of service agreement if enabled
        if (isTermsOfServiceEnabled() && agreeToTerms.isBlank()) {
            call.respondHtml(HttpStatusCode.BadRequest) {
                head { bootstrapHead("登録エラー") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"利用規約エラー" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-danger") {
                                            +"利用規約への同意は必須です。"
                                        }
                                        a(href = "/register", classes = "btn btn-primary") { +"戻る" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
            return@post
        }
        
        val user = userService.register(username, password, email)
        if (user != null) {
            // Send welcome email
            emailService.sendWelcomeEmail(email, username)
            
            // Send Slack notification for new user registration
            try {
                val userAgent = RequestContextHelper.extractUserAgent(call)
                val ipAddress = RequestContextHelper.extractIpAddress(call)
                slackService.sendNotification(
                    action = "新規ユーザー登録",
                    username = user.username,
                    userAgent = userAgent,
                    ipAddress = ipAddress,
                    additionalInfo = "メールアドレス: $email"
                )
            } catch (e: Exception) {
                // Log error but don't fail the registration process
                call.application.log.error("Failed to send Slack notification for registration", e)
            }
            
            call.sessions.set(UserSession(user.id, user.username))
            call.respondRedirect("/")
        } else {
            call.respondHtml(HttpStatusCode.Conflict) {
                head { bootstrapHead("登録エラー") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"登録エラー" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-warning") {
                                            +"そのユーザー名またはメールアドレスは既に使用されています。"
                                        }
                                        a(href = "/register", classes = "btn btn-primary") { +"戻る" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
        }
    }
    
    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/")
    }
    
    // Password reset routes
    get("/forgot-password") {
        call.respondHtml {
            head { bootstrapHead("パスワードリセット") }
            body(classes = "d-flex flex-column min-vh-100") {
                addGTMBodyScript()
                div(classes = "container mt-5") {
                    div(classes = "row justify-content-center") {
                        div(classes = "col-md-6") {
                            div(classes = "card") {
                                div(classes = "card-header") {
                                    h1(classes = "card-title mb-0") { +"パスワードリセット" }
                                }
                                div(classes = "card-body") {
                                    p { +"パスワードをリセットするには、登録済みのメールアドレスを入力してください。" }
                                    form(action = "/forgot-password", method = FormMethod.post) {
                                        div(classes = "mb-3") {
                                            label(classes = "form-label") { +"メールアドレス" }
                                            emailInput(classes = "form-control") { 
                                                name = "email"
                                                placeholder = "メールアドレスを入力してください"
                                                required = true
                                            }
                                        }
                                        div(classes = "d-grid") {
                                            submitInput(classes = "btn btn-warning") { value = "リセットメールを送信" }
                                        }
                                    }
                                    hr()
                                    div(classes = "text-center") {
                                        a(href = "/login", classes = "btn btn-link") { +"ログイン画面に戻る" }
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
    
    post("/forgot-password") {
        val params = call.receiveParameters()
        val email = params["email"] ?: ""
        
        if (email.isBlank()) {
            call.respondHtml(HttpStatusCode.BadRequest) {
                head { bootstrapHead("エラー") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"入力エラー" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-danger") {
                                            +"メールアドレスは必須です。"
                                        }
                                        a(href = "/forgot-password", classes = "btn btn-primary") { +"戻る" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
            return@post
        }
        
        // Always show success message for security (don't reveal if email exists)
        val token = userService.requestPasswordReset(email)
        if (token != null) {
            emailService.sendPasswordResetEmail(email, token)
        }
        
        call.respondHtml {
            head { bootstrapHead("メール送信完了") }
            body(classes = "d-flex flex-column min-vh-100") {
                addGTMBodyScript()
                div(classes = "container mt-5") {
                    div(classes = "row justify-content-center") {
                        div(classes = "col-md-6") {
                            div(classes = "card") {
                                div(classes = "card-header") {
                                    h1(classes = "card-title mb-0") { +"メール送信完了" }
                                }
                                div(classes = "card-body") {
                                    div(classes = "alert alert-success") {
                                        +"パスワードリセット用のメールを送信しました。メールを確認してください。"
                                    }
                                    p { +"メールが届かない場合は、迷惑メールフォルダを確認してください。" }
                                    a(href = "/login", classes = "btn btn-primary") { +"ログイン画面へ" }
                                }
                            }
                        }
                    }
                }
                addFooter()
            }
        }
    }
    
    get("/reset-password") {
        val token = call.request.queryParameters["token"] ?: ""
        
        if (token.isBlank()) {
            call.respondRedirect("/login")
            return@get
        }
        
        call.respondHtml {
            head { bootstrapHead("新しいパスワード") }
            body(classes = "d-flex flex-column min-vh-100") {
                addGTMBodyScript()
                div(classes = "container mt-5") {
                    div(classes = "row justify-content-center") {
                        div(classes = "col-md-6") {
                            div(classes = "card") {
                                div(classes = "card-header") {
                                    h1(classes = "card-title mb-0") { +"新しいパスワードの設定" }
                                }
                                div(classes = "card-body") {
                                    form(action = "/reset-password", method = FormMethod.post) {
                                        hiddenInput {
                                            name = "token"
                                            value = token
                                        }
                                        div(classes = "mb-3") {
                                            label(classes = "form-label") { +"新しいパスワード" }
                                            passwordInput(classes = "form-control") { 
                                                name = "password"
                                                placeholder = "新しいパスワードを入力してください"
                                                required = true
                                                minLength = "6"
                                            }
                                        }
                                        div(classes = "mb-3") {
                                            label(classes = "form-label") { +"パスワード確認" }
                                            passwordInput(classes = "form-control") { 
                                                name = "confirmPassword"
                                                placeholder = "パスワードを再度入力してください"
                                                required = true
                                                minLength = "6"
                                            }
                                        }
                                        div(classes = "d-grid") {
                                            submitInput(classes = "btn btn-success") { value = "パスワードを更新" }
                                        }
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
    
    post("/reset-password") {
        val params = call.receiveParameters()
        val token = params["token"] ?: ""
        val password = params["password"] ?: ""
        val confirmPassword = params["confirmPassword"] ?: ""
        
        if (token.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            call.respondHtml(HttpStatusCode.BadRequest) {
                head { bootstrapHead("エラー") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"入力エラー" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-danger") {
                                            +"すべての項目は必須です。"
                                        }
                                        a(href = "/reset-password?token=$token", classes = "btn btn-primary") { +"戻る" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
            return@post
        }
        
        if (password != confirmPassword) {
            call.respondHtml(HttpStatusCode.BadRequest) {
                head { bootstrapHead("エラー") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"パスワード不一致" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-danger") {
                                            +"パスワードが一致しません。"
                                        }
                                        a(href = "/reset-password?token=$token", classes = "btn btn-primary") { +"戻る" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
            return@post
        }
        
        val success = userService.resetPassword(token, password)
        if (success) {
            call.respondHtml {
                head { bootstrapHead("パスワード更新完了") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"パスワード更新完了" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-success") {
                                            +"パスワードが正常に更新されました。"
                                        }
                                        a(href = "/login", classes = "btn btn-primary") { +"ログインする" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
        } else {
            call.respondHtml(HttpStatusCode.BadRequest) {
                head { bootstrapHead("エラー") }
                body(classes = "d-flex flex-column min-vh-100") {
                    addGTMBodyScript()
                    div(classes = "container mt-5") {
                        div(classes = "row justify-content-center") {
                            div(classes = "col-md-6") {
                                div(classes = "card") {
                                    div(classes = "card-header") {
                                        h1(classes = "card-title mb-0") { +"トークンエラー" }
                                    }
                                    div(classes = "card-body") {
                                        div(classes = "alert alert-danger") {
                                            +"無効なトークンまたは期限が切れています。"
                                        }
                                        a(href = "/forgot-password", classes = "btn btn-primary") { +"新しくリセットを申請" }
                                    }
                                }
                            }
                        }
                    }
                    addFooter()
                }
            }
        }
    }
}
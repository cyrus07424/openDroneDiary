package routing

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.http.*
import com.opendronediary.model.UserSession
import com.opendronediary.service.UserService
import com.opendronediary.service.RegisterResult
import com.opendronediary.service.ConfirmRegistrationResult
import com.opendronediary.service.ResetPasswordResult
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

// Helper function to add form submission modal dialog and JavaScript
fun BODY.addFormSubmissionModal() {
    // Modal dialog for form submission loading
    div(classes = "modal fade") {
        id = "loadingModal"
        attributes["data-bs-backdrop"] = "static"
        attributes["data-bs-keyboard"] = "false"
        div(classes = "modal-dialog modal-dialog-centered") {
            div(classes = "modal-content") {
                div(classes = "modal-body text-center py-4") {
                    div(classes = "spinner-border text-primary mb-3") {
                        attributes["role"] = "status"
                        span(classes = "visually-hidden") { +"読み込み中..." }
                    }
                    h5(classes = "modal-title") { 
                        id = "loadingModalMessage"
                        +"処理中です..." 
                    }
                    p(classes = "text-muted mb-0") { +"しばらくお待ちください" }
                }
            }
        }
    }
    
    // JavaScript for form submission handling
    script(type = "text/javascript") {
        unsafe {
            +"""
            // Form submission messages mapping
            const FORM_MESSAGES = {
                'create': '登録中です...',
                'update': '更新中です...',
                'delete': '削除中です...',
                'login': 'ログイン中です...',
                'register': '登録中です...',
                'reset': 'メール送信中です...',
                'password-update': 'パスワード更新中です...',
                'default': '処理中です...'
            };
            
            // Function to get appropriate message based on form action and content
            function getFormMessage(form) {
                const action = form.getAttribute('action') || '';
                const submitValue = form.querySelector('input[type="submit"]')?.value || '';
                
                // Check for delete action
                if (form.querySelector('input[name="_method"][value="delete"]') || 
                    submitValue.includes('削除')) {
                    return FORM_MESSAGES.delete;
                }
                
                // Check by submit button text
                if (submitValue.includes('ログイン')) return FORM_MESSAGES.login;
                if (submitValue.includes('登録')) return FORM_MESSAGES.register;
                if (submitValue.includes('更新')) return FORM_MESSAGES.update;
                if (submitValue.includes('追加')) return FORM_MESSAGES.create;
                if (submitValue.includes('リセット') || submitValue.includes('送信')) return FORM_MESSAGES.reset;
                if (submitValue.includes('パスワード')) return FORM_MESSAGES['password-update'];
                
                // Check by URL path
                if (action.includes('/login')) return FORM_MESSAGES.login;
                if (action.includes('/register')) return FORM_MESSAGES.register;
                if (action.includes('/forgot-password')) return FORM_MESSAGES.reset;
                if (action.includes('/reset-password')) return FORM_MESSAGES['password-update'];
                
                // Default for POST to creation endpoints
                if (action && !action.includes('/ui/')) return FORM_MESSAGES.create;
                
                return FORM_MESSAGES.default;
            }
            
            // Add event listeners to all forms when DOM is loaded
            document.addEventListener('DOMContentLoaded', function() {
                const forms = document.querySelectorAll('form');
                const modal = new bootstrap.Modal(document.getElementById('loadingModal'));
                const modalMessage = document.getElementById('loadingModalMessage');
                
                forms.forEach(function(form) {
                    form.addEventListener('submit', function(e) {
                        // Get the submit button that was clicked
                        const submitButton = form.querySelector('input[type="submit"]');
                        
                        // Disable the submit button to prevent double-clicking
                        if (submitButton) {
                            submitButton.disabled = true;
                        }
                        
                        // Set appropriate message and show modal
                        const message = getFormMessage(form);
                        modalMessage.textContent = message;
                        modal.show();
                        
                        // Re-enable submit button after a delay in case of errors
                        setTimeout(function() {
                            if (submitButton) {
                                submitButton.disabled = false;
                            }
                        }, 5000);
                    });
                });
            });
            """
        }
    }
}

// Helper function to add password strength meter CSS and JavaScript
fun BODY.addPasswordStrengthMeter() {
    // Add CSS for password strength meter
    style(type = "text/css") {
        unsafe {
            +"""
            .password-strength-bar {
                width: 100%;
                height: 8px;
                background-color: #e0e0e0;
                border-radius: 4px;
                overflow: hidden;
            }
            
            .password-strength-fill {
                height: 100%;
                transition: width 0.3s ease, background-color 0.3s ease;
                width: 0%;
                background-color: #dc3545;
            }
            
            .password-strength-text {
                font-size: 0.875rem;
                font-weight: 500;
            }
            
            .strength-very-weak .password-strength-fill { background-color: #dc3545; width: 20%; }
            .strength-weak .password-strength-fill { background-color: #fd7e14; width: 40%; }
            .strength-fair .password-strength-fill { background-color: #ffc107; width: 60%; }
            .strength-good .password-strength-fill { background-color: #198754; width: 80%; }
            .strength-strong .password-strength-fill { background-color: #0d6efd; width: 100%; }
            
            .text-very-weak { color: #dc3545; }
            .text-weak { color: #fd7e14; }
            .text-fair { color: #ffc107; }
            .text-good { color: #198754; }
            .text-strong { color: #0d6efd; }
            """
        }
    }
    
    // Add JavaScript for password strength checking
    script(type = "text/javascript") {
        unsafe {
            +"""
            function checkPasswordStrength(passwordFieldId, meterContainerId) {
                const password = document.getElementById(passwordFieldId).value;
                const meterContainer = document.getElementById(meterContainerId);
                
                // Determine element IDs based on the meter container
                let strengthFillId, strengthTextId;
                if (meterContainerId === 'passwordStrengthMeter') {
                    strengthFillId = 'passwordStrengthFill';
                    strengthTextId = 'passwordStrengthText';
                } else if (meterContainerId === 'resetPasswordStrengthMeter') {
                    strengthFillId = 'resetPasswordStrengthFill';
                    strengthTextId = 'resetPasswordStrengthText';
                } else {
                    return; // Unknown meter container
                }
                
                const strengthFill = document.getElementById(strengthFillId);
                const strengthText = document.getElementById(strengthTextId);
                
                if (!strengthFill || !strengthText) return;
                
                if (password.length === 0) {
                    meterContainer.style.display = 'none';
                    return;
                }
                
                meterContainer.style.display = 'block';
                
                // Simple client-side strength calculation (basic heuristics)
                let score = 0;
                let feedback = '';
                
                // Length check
                if (password.length >= 8) score += 1;
                if (password.length >= 12) score += 1;
                
                // Character variety checks
                if (/[a-z]/.test(password)) score += 1;
                if (/[A-Z]/.test(password)) score += 1;
                if (/[0-9]/.test(password)) score += 1;
                if (/[^A-Za-z0-9]/.test(password)) score += 1;
                
                // Common patterns penalty
                if (/^(password|123456|qwerty)/i.test(password)) score = Math.max(0, score - 2);
                
                // Remove all previous strength classes
                meterContainer.classList.remove('strength-very-weak', 'strength-weak', 'strength-fair', 'strength-good', 'strength-strong');
                strengthText.classList.remove('text-very-weak', 'text-weak', 'text-fair', 'text-good', 'text-strong');
                
                if (score <= 1) {
                    meterContainer.classList.add('strength-very-weak');
                    strengthText.classList.add('text-very-weak');
                    feedback = '非常に弱いパスワードです';
                } else if (score <= 2) {
                    meterContainer.classList.add('strength-weak');
                    strengthText.classList.add('text-weak');
                    feedback = '弱いパスワードです';
                } else if (score <= 3) {
                    meterContainer.classList.add('strength-fair');
                    strengthText.classList.add('text-fair');
                    feedback = '普通のパスワードです';
                } else if (score <= 4) {
                    meterContainer.classList.add('strength-good');
                    strengthText.classList.add('text-good');
                    feedback = '良いパスワードです';
                } else {
                    meterContainer.classList.add('strength-strong');
                    strengthText.classList.add('text-strong');
                    feedback = '非常に強いパスワードです';
                }
                
                strengthText.textContent = feedback;
            }
            """
        }
    }
}

// Helper function to create Bootstrap head with CDN links and SEO meta tags
fun HEAD.bootstrapHead(pageTitle: String, includeSEO: Boolean = false) {
    val fullTitle = if (pageTitle.contains("OpenDroneDiary")) pageTitle else "$pageTitle - OpenDroneDiary"
    title { +fullTitle }
    meta(charset = "utf-8")
    meta(name = "viewport", content = "width=device-width, initial-scale=1")
    
    // SEO meta tags for public pages
    if (includeSEO) {
        meta(name = "description", content = "ドローンの飛行日誌を管理するためのオープンソースのツールです。飛行記録、日常点検、整備記録を簡単に管理できます。")
        meta(name = "keywords", content = "ドローン,飛行日誌,飛行記録,点検記録,整備記録,オープンソース,drone,flight log,inspection")
        meta(name = "author", content = "OpenDroneDiary")
        meta(name = "robots", content = "index, follow")
        
        // Open Graph meta tags for social sharing
        meta {
            attributes["property"] = "og:title"
            attributes["content"] = fullTitle
        }
        meta {
            attributes["property"] = "og:description"
            attributes["content"] = "ドローンの飛行日誌を管理するためのオープンソースのツールです。飛行記録、日常点検、整備記録を簡単に管理できます。"
        }
        meta {
            attributes["property"] = "og:type"
            attributes["content"] = "website"
        }
        meta {
            attributes["property"] = "og:site_name"
            attributes["content"] = "OpenDroneDiary"
        }
        
        // Twitter Card meta tags
        meta(name = "twitter:card", content = "summary")
        meta(name = "twitter:title", content = fullTitle)
        meta(name = "twitter:description", content = "ドローンの飛行日誌を管理するためのオープンソースのツールです。")
    }
    
    link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css")
    script(src = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js") { }
    addGTMHeadScript()
}

fun Route.configureTopAndAuthRouting(userService: UserService, emailService: EmailService, slackService: SlackService) {
    get("/") {
        val session = call.sessions.get<UserSession>()
        call.respondHtml {
            head { bootstrapHead("トップ", includeSEO = true) }
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
                                            a(href = "/pilots/ui", classes = "btn btn-info") { +"👨‍✈️ パイロット管理" }
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
            head { bootstrapHead("ログイン", includeSEO = true) }
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
                addFormSubmissionModal()
                addFooter()
            }
        }
    }
    
    post("/login") {
        val params = call.receiveParameters()
        val email = params["email"] ?: ""
        val password = params["password"] ?: ""
        
        val user = userService.loginByEmail(email, password)
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
                                            +"メールアドレスまたはパスワードが間違っています。"
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
            head { bootstrapHead("ユーザー登録", includeSEO = true) }
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
                                                id = "registerPassword"
                                                placeholder = "パスワードを入力してください"
                                                required = true
                                                attributes["onkeyup"] = "checkPasswordStrength('registerPassword', 'passwordStrengthMeter')"
                                            }
                                            // Password strength meter
                                            div(classes = "mt-2") {
                                                div(classes = "password-strength-meter") {
                                                    id = "passwordStrengthMeter"
                                                    style = "display: none;"
                                                    div(classes = "password-strength-bar") {
                                                        div(classes = "password-strength-fill") {
                                                            id = "passwordStrengthFill"
                                                        }
                                                    }
                                                    div(classes = "password-strength-text mt-1") {
                                                        id = "passwordStrengthText"
                                                    }
                                                }
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
                addPasswordStrengthMeter()
                addFormSubmissionModal()
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
        
        val result = userService.register(username, password, email)
        when (result) {
            is RegisterResult.PendingVerification -> {
                // Send verification email
                emailService.sendRegistrationVerificationEmail(email, username, result.token)
                
                // Send Slack notification for provisional registration
                try {
                    val userAgent = RequestContextHelper.extractUserAgent(call)
                    val ipAddress = RequestContextHelper.extractIpAddress(call)
                    slackService.sendNotification(
                        action = "仮登録",
                        username = username,
                        userAgent = userAgent,
                        ipAddress = ipAddress,
                        additionalInfo = "メールアドレス: $email"
                    )
                } catch (e: Exception) {
                    // Log error but don't fail the registration process
                    call.application.log.error("Failed to send Slack notification for provisional registration", e)
                }
                
                call.respondHtml {
                    head { bootstrapHead("確認メール送信完了") }
                    body(classes = "d-flex flex-column min-vh-100") {
                        addGTMBodyScript()
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-6") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"確認メール送信完了" }
                                        }
                                        div(classes = "card-body") {
                                            div(classes = "alert alert-success") {
                                                +"確認メールを ${result.email} に送信しました。メール内のリンクをクリックして登録を完了してください。"
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
            is RegisterResult.Failure -> {
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
                                            div(classes = "alert alert-danger") {
                                                +result.message
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
            is RegisterResult.WeakPassword -> {
                call.respondHtml(HttpStatusCode.BadRequest) {
                    head { bootstrapHead("パスワード強度エラー") }
                    body(classes = "d-flex flex-column min-vh-100") {
                        addGTMBodyScript()
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-6") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"パスワード強度不足" }
                                        }
                                        div(classes = "card-body") {
                                            div(classes = "alert alert-warning") {
                                                strong { +result.validation.feedback }
                                                if (result.validation.suggestions.isNotEmpty()) {
                                                    br()
                                                    +"推奨：${result.validation.suggestions}"
                                                }
                                                if (result.validation.warning.isNotEmpty()) {
                                                    br()
                                                    +"警告：${result.validation.warning}"
                                                }
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
    }
    
    get("/verify-email") {
        val token = call.request.queryParameters["token"] ?: ""
        
        if (token.isBlank()) {
            call.respondRedirect("/register")
            return@get
        }
        
        val result = userService.confirmRegistration(token)
        when (result) {
            is ConfirmRegistrationResult.Success -> {
                val user = result.user
                // Send welcome email
                emailService.sendWelcomeEmail(user.email, user.username)
                
                // Send Slack notification for completed registration
                try {
                    val userAgent = RequestContextHelper.extractUserAgent(call)
                    val ipAddress = RequestContextHelper.extractIpAddress(call)
                    slackService.sendNotification(
                        action = "新規ユーザー登録完了",
                        username = user.username,
                        userAgent = userAgent,
                        ipAddress = ipAddress,
                        additionalInfo = "メールアドレス: ${user.email}"
                    )
                } catch (e: Exception) {
                    call.application.log.error("Failed to send Slack notification for registration completion", e)
                }
                
                call.sessions.set(UserSession(user.id, user.username))
                call.respondRedirect("/")
            }
            is ConfirmRegistrationResult.InvalidToken -> {
                call.respondHtml(HttpStatusCode.BadRequest) {
                    head { bootstrapHead("認証エラー") }
                    body(classes = "d-flex flex-column min-vh-100") {
                        addGTMBodyScript()
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-6") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"認証エラー" }
                                        }
                                        div(classes = "card-body") {
                                            div(classes = "alert alert-danger") {
                                                +"確認リンクが無効または期限切れです。再度ユーザー登録を行ってください。"
                                            }
                                            a(href = "/register", classes = "btn btn-primary") { +"ユーザー登録へ" }
                                        }
                                    }
                                }
                            }
                        }
                        addFooter()
                    }
                }
            }
            is ConfirmRegistrationResult.Failure -> {
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
                                            div(classes = "alert alert-danger") {
                                                +result.message
                                            }
                                            a(href = "/register", classes = "btn btn-primary") { +"ユーザー登録へ" }
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
    
    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/")
    }
    
    // Password reset routes
    get("/forgot-password") {
        call.respondHtml {
            head { bootstrapHead("パスワードリセット", includeSEO = true) }
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
            head { bootstrapHead("新しいパスワード", includeSEO = true) }
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
                                                id = "resetPassword"
                                                placeholder = "新しいパスワードを入力してください"
                                                required = true
                                                minLength = "6"
                                                attributes["onkeyup"] = "checkPasswordStrength('resetPassword', 'resetPasswordStrengthMeter')"
                                            }
                                            // Password strength meter
                                            div(classes = "mt-2") {
                                                div(classes = "password-strength-meter") {
                                                    id = "resetPasswordStrengthMeter"
                                                    style = "display: none;"
                                                    div(classes = "password-strength-bar") {
                                                        div(classes = "password-strength-fill") {
                                                            id = "resetPasswordStrengthFill"
                                                        }
                                                    }
                                                    div(classes = "password-strength-text mt-1") {
                                                        id = "resetPasswordStrengthText"
                                                    }
                                                }
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
                addPasswordStrengthMeter()
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
        
        val result = userService.resetPassword(token, password)
        when (result) {
            is ResetPasswordResult.Success -> {
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
            }
            is ResetPasswordResult.InvalidToken -> {
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
            is ResetPasswordResult.Failure -> {
                call.respondHtml(HttpStatusCode.InternalServerError) {
                    head { bootstrapHead("エラー") }
                    body(classes = "d-flex flex-column min-vh-100") {
                        addGTMBodyScript()
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-6") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"システムエラー" }
                                        }
                                        div(classes = "card-body") {
                                            div(classes = "alert alert-danger") {
                                                +"パスワードの更新に失敗しました。しばらく待ってから再試行してください。"
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
            }
            is ResetPasswordResult.WeakPassword -> {
                call.respondHtml(HttpStatusCode.BadRequest) {
                    head { bootstrapHead("パスワード強度エラー") }
                    body(classes = "d-flex flex-column min-vh-100") {
                        addGTMBodyScript()
                        div(classes = "container mt-5") {
                            div(classes = "row justify-content-center") {
                                div(classes = "col-md-6") {
                                    div(classes = "card") {
                                        div(classes = "card-header") {
                                            h1(classes = "card-title mb-0") { +"パスワード強度不足" }
                                        }
                                        div(classes = "card-body") {
                                            div(classes = "alert alert-warning") {
                                                strong { +result.validation.feedback }
                                                if (result.validation.suggestions.isNotEmpty()) {
                                                    br()
                                                    +"推奨：${result.validation.suggestions}"
                                                }
                                                if (result.validation.warning.isNotEmpty()) {
                                                    br()
                                                    +"警告：${result.validation.warning}"
                                                }
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
            }
        }
    }
}
package utils

import kotlinx.html.*

object PolicyHelper {
    private val termsOfServiceUrl: String? = System.getenv("TERMS_OF_SERVICE_URL")
    private val privacyPolicyUrl: String? = System.getenv("PRIVACY_POLICY_URL")
    
    fun isTermsOfServiceEnabled(): Boolean = !termsOfServiceUrl.isNullOrBlank()
    fun isPrivacyPolicyEnabled(): Boolean = !privacyPolicyUrl.isNullOrBlank()
    
    fun getTermsOfServiceUrl(): String? = termsOfServiceUrl
    fun getPrivacyPolicyUrl(): String? = privacyPolicyUrl
    
    fun BODY.addFooter() {
        if (isTermsOfServiceEnabled() || isPrivacyPolicyEnabled()) {
            footer(classes = "mt-auto py-3 bg-light border-top") {
                div(classes = "container") {
                    div(classes = "row") {
                        div(classes = "col-12 text-center") {
                            small(classes = "text-muted") {
                                val links = mutableListOf<String>()
                                if (isTermsOfServiceEnabled()) {
                                    links.add("""<a href="$termsOfServiceUrl" target="_blank" class="text-decoration-none">利用規約</a>""")
                                }
                                if (isPrivacyPolicyEnabled()) {
                                    links.add("""<a href="$privacyPolicyUrl" target="_blank" class="text-decoration-none">プライバシーポリシー</a>""")
                                }
                                unsafe {
                                    raw(links.joinToString(" | "))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
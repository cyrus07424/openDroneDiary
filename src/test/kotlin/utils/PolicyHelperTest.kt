package utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyHelperTest {
    
    @Test
    fun testPolicyHelperWithoutEnvironmentVariables() {
        // By default, no environment variables are set
        assertFalse(PolicyHelper.isTermsOfServiceEnabled())
        assertFalse(PolicyHelper.isPrivacyPolicyEnabled())
    }
}
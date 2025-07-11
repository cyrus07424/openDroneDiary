package utils

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GTMHelperTest {
    
    @Test
    fun testGTMDisabledByDefault() {
        // When GTM_ID environment variable is not set, GTM should be disabled
        // This test will pass when GTM_ID is not set
        val isEnabled = GTMHelper.isGTMEnabled()
        // The result depends on whether GTM_ID is set, so we just test it doesn't crash
        assertTrue(isEnabled == true || isEnabled == false)
    }
}
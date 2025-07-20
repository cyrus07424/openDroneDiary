package utils

import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class ErrorPageHelperTest {
    
    @Test
    fun testErrorPageHelperExists() {
        // Basic test to ensure ErrorPageHelper class is available
        assertNotNull(ErrorPageHelper)
        assertTrue(ErrorPageHelper::class.simpleName == "ErrorPageHelper")
    }
}
package utils

import io.ktor.server.application.*
import io.ktor.server.request.*

object RequestContextHelper {
    
    fun extractUserAgent(call: ApplicationCall): String? {
        return call.request.header("User-Agent")
    }
    
    fun extractIpAddress(call: ApplicationCall): String? {
        // Check for X-Forwarded-For header first (for reverse proxies)
        val xForwardedFor = call.request.header("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            // Take the first IP in case of multiple comma-separated IPs
            return xForwardedFor.split(",").firstOrNull()?.trim()
        }
        
        // Check for X-Real-IP header
        val xRealIp = call.request.header("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }
        
        // Fallback to remote host
        return call.request.local.remoteHost
    }
}
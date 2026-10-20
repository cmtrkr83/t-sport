package com.tsport.links.util

object CookieUtils {

    fun hasAuthToken(cookieHeader: String?): Boolean = valueOf(cookieHeader, "auth_token") != null

    fun csrfToken(cookieHeader: String?): String? = valueOf(cookieHeader, "ct0")

    private fun valueOf(cookieHeader: String?, name: String): String? {
        if (cookieHeader.isNullOrBlank()) return null
        return cookieHeader.split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotEmpty() }
    }
}

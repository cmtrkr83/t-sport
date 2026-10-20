package com.tsport.links.auth

import android.webkit.CookieManager
import com.tsport.links.util.CookieUtils

class AuthRepository(
    private val cookieManager: CookieManager = CookieManager.getInstance()
) {

    fun isLoggedIn(): Boolean = CookieUtils.hasAuthToken(cookie())

    fun csrfToken(): String? = CookieUtils.csrfToken(cookie())

    fun logout(onComplete: () -> Unit) {
        cookieManager.removeAllCookies {
            cookieManager.flush()
            onComplete()
        }
    }

    private fun cookie(): String? = cookieManager.getCookie(ORIGIN)

    companion object {
        const val ORIGIN = "https://x.com"
    }
}

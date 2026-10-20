package com.tsport.links.search

class PageLoadException : Exception()

class SessionExpiredException : Exception()

fun isLoginUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return url.contains("/i/flow/login") ||
        url.contains("/login") ||
        url.contains("/i/flow/signup")
}

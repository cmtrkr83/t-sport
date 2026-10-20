package com.tsport.links.links

fun interface UrlResolver {
    suspend fun resolve(url: String): String?
}

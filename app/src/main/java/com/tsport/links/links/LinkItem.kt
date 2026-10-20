package com.tsport.links.links

data class LinkItem(
    val url: String,
    val domain: String,
    val sourceTweetUrl: String? = null
)

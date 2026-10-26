package com.tsport.links.ui

import com.tsport.links.search.SearchOptions
import com.tsport.links.search.SearchTab

internal fun shouldShowTabFallback(options: SearchOptions?, tabFallback: Boolean): Boolean =
    options?.tab == SearchTab.LATEST && tabFallback

internal fun withTabWarning(message: String?, warning: String?): String? =
    if (message == null || warning == null) message else "$message · $warning"

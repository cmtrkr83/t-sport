package com.tsport.links.matchday

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object IstanbulDate {
    fun today(nowMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Istanbul")
        }.format(Date(nowMillis))
}

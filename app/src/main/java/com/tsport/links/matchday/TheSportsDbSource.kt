package com.tsport.links.matchday

import com.tsport.links.links.TcoResolver
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

class TheSportsDbSource(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = BASE_URL
) : MatchDaySource {

    override suspend fun load(date: String): Result<List<TodayMatch>> =
        withContext(Dispatchers.IO) {
            try {
                val url = baseUrl.toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.addQueryParameter("d", date)
                    ?.addQueryParameter("l", LEAGUE_ID)
                    ?.build()
                    ?: return@withContext Result.failure(IllegalArgumentException("Geçersiz API adresi"))
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", TcoResolver.USER_AGENT)
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Result.failure(IOException("HTTP ${response.code}"))
                    } else {
                        val body = response.body?.string()
                        if (body == null) {
                            Result.failure(IOException("Boş yanıt"))
                        } else {
                            Result.success(MatchDayParser.parse(body))
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    companion object {
        const val BASE_URL = "https://www.thesportsdb.com/api/v1/json/123/eventsday.php"
        const val LEAGUE_ID = "4339"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}

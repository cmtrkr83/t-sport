package com.tsport.links.links

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Priority account list with a three-layer fallback:
 * 1. the list fetched by the last successful [refresh],
 * 2. the cached remote JSON on disk,
 * 3. the bundled asset shipped with the APK.
 * A failed or malformed remote update never clears the current list.
 */
class PriorityAccountsRepository(
    private val client: OkHttpClient = defaultClient(),
    private val bundled: () -> String,
    private val cacheFile: File,
    private val remoteUrl: String = REMOTE_URL
) {

    private val etagFile = File(cacheFile.parentFile, "${cacheFile.name}.etag")
    private val mutex = Mutex()

    @Volatile
    private var accounts: List<PriorityAccount>? = null

    @Volatile
    private var etag: String? = null

    /** Current list: last refreshed value, cached file, or bundled asset. */
    fun get(): List<PriorityAccount> {
        accounts?.let { return it }
        val loaded = readCache() ?: PriorityAccounts.parse(bundled())
        accounts = loaded
        return loaded
    }

    /** Fetches the remote list; on any failure keeps the current one. */
    suspend fun refresh(): List<PriorityAccount> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = get()
            try {
                val storedEtag = etag ?: readEtag()
                val request = Request.Builder()
                    .url(remoteUrl)
                    .header("User-Agent", USER_AGENT)
                    .apply { storedEtag?.let { header("If-None-Match", it) } }
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == HTTP_NOT_MODIFIED -> current
                        response.isSuccessful -> {
                            val body = response.body?.string()
                            val parsed = PriorityAccounts.parse(body)
                            if (body.isNullOrBlank() || parsed.isEmpty()) {
                                current
                            } else {
                                val header = response.header("ETag")
                                accounts = parsed
                                etag = header
                                runCatching {
                                    cacheFile.writeText(body)
                                    header?.let { etagFile.writeText(it) }
                                }
                                parsed
                            }
                        }
                        else -> current
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                current
            }
        }
    }

    private fun readCache(): List<PriorityAccount>? =
        if (!cacheFile.exists()) {
            null
        } else {
            PriorityAccounts.parse(runCatching { cacheFile.readText() }.getOrNull())
                .takeIf { it.isNotEmpty() }
        }

    private fun readEtag(): String? =
        if (!etagFile.exists()) {
            null
        } else {
            runCatching { etagFile.readText().trim() }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
        }

    companion object {
        const val REMOTE_URL =
            "https://raw.githubusercontent.com/cmtrkr83/t-sport/main/assets/priority_accounts.json"
        private const val HTTP_NOT_MODIFIED = 304
        private const val USER_AGENT = "TSport/1.0 (priority-accounts)"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }
}

package com.tsport.links.links

import org.json.JSONObject

data class PriorityAccount(
    val handle: String,
    val name: String? = null
) {
    val normalizedHandle: String get() = handle.trim().removePrefix("@").lowercase()
    val normalizedName: String? get() = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
}

object PriorityAccounts {

    /**
     * Parses a priority list of the shape
     * `{"accounts": [{"handle": "X", "name": "optional"}, ...]}`.
     * A bare string entry or an unknown shape yields an empty list, so a
     * broken remote file can never disable the fallback lists.
     */
    fun parse(json: String?): List<PriorityAccount> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val root = JSONObject(json)
            val array = root.optJSONArray("accounts") ?: return emptyList()
            val seen = HashSet<String>()
            val accounts = ArrayList<PriorityAccount>()
            for (index in 0 until array.length()) {
                val item = array.opt(index) as? JSONObject ?: continue
                val handle = item.optString("handle").trim().removePrefix("@")
                if (handle.isEmpty()) continue
                if (!seen.add(handle.lowercase())) continue
                val name = item.optString("name").trim().takeIf { it.isNotEmpty() }
                accounts += PriorityAccount(handle, name)
            }
            accounts
        } catch (_: Exception) {
            emptyList()
        }
    }
}

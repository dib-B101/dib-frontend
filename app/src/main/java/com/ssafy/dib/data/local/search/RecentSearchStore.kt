package com.ssafy.dib.data.local.search

import android.content.Context

/**
 * 최근 검색어. 기기에만 저장하는 개인 기록이라 서버로 보내지 않는다.
 * 검색 화면이 보여주던 고정 목록("필름 카메라" …)을 실제 검색 기록으로 바꾸기 위한 저장소다.
 */
class RecentSearchStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> =
        preferences.getString(KEY_KEYWORDS, null)
            ?.split(SEPARATOR)
            ?.filter(String::isNotBlank)
            ?: emptyList()

    // 같은 검색어는 맨 앞으로 올리고, 오래된 것부터 잘라 최대 MAX_KEYWORDS 개만 남긴다
    fun add(keyword: String): List<String> {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return load()
        val next = (listOf(trimmed) + load()).distinct().take(MAX_KEYWORDS)
        save(next)
        return next
    }

    fun remove(keyword: String): List<String> {
        val next = load().filterNot { it == keyword }
        save(next)
        return next
    }

    fun clear(): List<String> {
        preferences.edit().remove(KEY_KEYWORDS).apply()
        return emptyList()
    }

    private fun save(keywords: List<String>) {
        preferences.edit().putString(KEY_KEYWORDS, keywords.joinToString(SEPARATOR)).apply()
    }

    companion object {
        const val MAX_KEYWORDS = 10
        private const val PREFERENCES_NAME = "dib_recent_search"
        private const val KEY_KEYWORDS = "keywords"
        // 검색어에 들어갈 일이 없는 제어 문자로 이어 붙인다
        private const val SEPARATOR = ""
    }
}

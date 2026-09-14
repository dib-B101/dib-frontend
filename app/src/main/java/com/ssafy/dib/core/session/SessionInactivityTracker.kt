package com.ssafy.dib.core.session

import android.content.Context

class SessionInactivityTracker(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private var expiredBeforeFirstInteraction = preferences.getLong(LAST_INTERACTION_KEY, 0L)
        .takeIf { it > 0L }
        ?.let { SessionInactivityPolicy.hasExpired(it, now(), com.ssafy.dib.BuildConfig.SESSION_IDLE_TIMEOUT_MILLIS) }
        ?: false

    fun startSession() {
        expiredBeforeFirstInteraction = false
        preferences.edit().putLong(LAST_INTERACTION_KEY, now()).apply()
    }

    fun endSession() {
        expiredBeforeFirstInteraction = false
        preferences.edit().remove(LAST_INTERACTION_KEY).apply()
    }

    fun recordInteraction() {
        if (preferences.contains(LAST_INTERACTION_KEY)) {
            preferences.edit().putLong(LAST_INTERACTION_KEY, now()).apply()
        }
    }

    fun hasExpired(timeoutMillis: Long = com.ssafy.dib.BuildConfig.SESSION_IDLE_TIMEOUT_MILLIS): Boolean {
        if (expiredBeforeFirstInteraction) return true
        val lastInteraction = preferences.getLong(LAST_INTERACTION_KEY, 0L)
        return lastInteraction > 0L && SessionInactivityPolicy.hasExpired(lastInteraction, now(), timeoutMillis)
    }

    private companion object {
        const val PREFERENCES_NAME = "dib_session_activity"
        const val LAST_INTERACTION_KEY = "last_interaction_at"
    }
}

object SessionInactivityPolicy {
    fun hasExpired(lastInteractionAt: Long, now: Long, timeoutMillis: Long): Boolean =
        timeoutMillis > 0L && now - lastInteractionAt >= timeoutMillis
}

package com.otaviobarreto.pokedex.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import java.time.LocalDate

object TrainerTodayStore {
    private const val PREFS = "trainer_today"
    private const val KEY_DATE = "date"
    private const val KEY_BASELINE_CAPTURED = "baseline_captured"
    private const val KEY_BASELINE_JOURNEY = "baseline_journey"
    private const val KEY_STREAK = "visit_streak"
    private const val KEY_RECENT_CAPTURES = "recent_captures"

    private var context: Context? = null

    var dayKey by mutableStateOf("")
        private set
    var baselineCaptured by mutableIntStateOf(0)
        private set
    var baselineJourneyCompleted by mutableIntStateOf(0)
        private set
    var visitStreak by mutableIntStateOf(1)
        private set
    var recentCapturedIds by mutableStateOf<List<Int>>(emptyList())
        private set

    fun initialize(context: Context, capturedCount: Int, journeyCompletedCount: Int) {
        this.context = context.applicationContext
        val prefs = this.context!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        recentCapturedIds = decodeIds(prefs.getString(KEY_RECENT_CAPTURES, "[]"))
        val today = LocalDate.now()
        val storedDate = prefs.getString(KEY_DATE, null)
        if (storedDate == today.toString()) {
            dayKey = storedDate
            baselineCaptured = prefs.getInt(KEY_BASELINE_CAPTURED, capturedCount)
            baselineJourneyCompleted = prefs.getInt(KEY_BASELINE_JOURNEY, journeyCompletedCount)
            visitStreak = prefs.getInt(KEY_STREAK, 1).coerceAtLeast(1)
        } else {
            rollDay(today, capturedCount, journeyCompletedCount, storedDate)
        }
    }

    fun ensureCurrentDay(capturedCount: Int, journeyCompletedCount: Int) {
        val today = LocalDate.now()
        if (dayKey == today.toString()) return
        rollDay(today, capturedCount, journeyCompletedCount, dayKey.ifBlank { null })
    }

    fun recordCapture(id: Int) {
        if (id !in 1..PokeApiService.MAX_NATIONAL_DEX_ID) return
        recentCapturedIds = (listOf(id) + recentCapturedIds.filterNot { it == id }).take(12)
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_RECENT_CAPTURES, JSONArray(recentCapturedIds).toString())
            ?.apply()
    }

    fun todayCaptured(currentCapturedCount: Int): Int =
        (currentCapturedCount - baselineCaptured).coerceAtLeast(0)

    fun todayJourneyCompleted(currentCompletedCount: Int): Int =
        (currentCompletedCount - baselineJourneyCompleted).coerceAtLeast(0)

    private fun rollDay(
        today: LocalDate,
        capturedCount: Int,
        journeyCompletedCount: Int,
        previousKey: String?
    ) {
        val prefs = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        val previousDate = previousKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val previousStreak = prefs.getInt(KEY_STREAK, 0)
        visitStreak = if (previousDate == today.minusDays(1)) {
            (previousStreak + 1).coerceAtLeast(1)
        } else {
            1
        }
        dayKey = today.toString()
        baselineCaptured = capturedCount
        baselineJourneyCompleted = journeyCompletedCount
        prefs.edit()
            .putString(KEY_DATE, dayKey)
            .putInt(KEY_BASELINE_CAPTURED, baselineCaptured)
            .putInt(KEY_BASELINE_JOURNEY, baselineJourneyCompleted)
            .putInt(KEY_STREAK, visitStreak)
            .apply()
    }

    private fun decodeIds(raw: String?): List<Int> = runCatching {
        val array = JSONArray(raw ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                array.optInt(i)
                    .takeIf { it in 1..PokeApiService.MAX_NATIONAL_DEX_ID }
                    ?.let(::add)
            }
        }
    }.getOrDefault(emptyList())
}

package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.ActivityItem
import com.example.domain.Prefs
import com.example.domain.RiskLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.checkFirstDataStore: DataStore<Preferences> by preferencesDataStore(name = "checkfirst_prefs")

class PreferencesDataStore(private val context: Context) {

    private object Keys {
        val TAMIL = booleanPreferencesKey("tamil")
        val SIMPLE_MODE = booleanPreferencesKey("simple_mode")
        val LARGE_TEXT = booleanPreferencesKey("large_text")
        val SAFETY_TIPS = booleanPreferencesKey("safety_tips")
        val COMPLETION_ALERTS = booleanPreferencesKey("completion_alerts")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val LOCAL_HISTORY_JSON = stringPreferencesKey("local_history_json")
    }

    val prefsFlow: Flow<Prefs> = context.checkFirstDataStore.data.map { p ->
        Prefs(
            tamil = p[Keys.TAMIL] ?: false,
            simpleMode = p[Keys.SIMPLE_MODE] ?: false,
            largeText = p[Keys.LARGE_TEXT] ?: false,
            safetyTips = p[Keys.SAFETY_TIPS] ?: true,
            completionAlerts = p[Keys.COMPLETION_ALERTS] ?: false,
            saveHistory = p[Keys.SAVE_HISTORY] ?: false,
        )
    }

    val localHistoryFlow: Flow<List<ActivityItem>> = context.checkFirstDataStore.data.map { p ->
        val raw = p[Keys.LOCAL_HISTORY_JSON].orEmpty()
        parseHistoryJson(raw)
    }

    suspend fun savePrefs(prefs: Prefs) {
        context.checkFirstDataStore.edit { p ->
            p[Keys.TAMIL] = prefs.tamil
            p[Keys.SIMPLE_MODE] = prefs.simpleMode
            p[Keys.LARGE_TEXT] = prefs.largeText
            p[Keys.SAFETY_TIPS] = prefs.safetyTips
            p[Keys.COMPLETION_ALERTS] = prefs.completionAlerts
            p[Keys.SAVE_HISTORY] = prefs.saveHistory
            if (!prefs.saveHistory) {
                p.remove(Keys.LOCAL_HISTORY_JSON)
            }
        }
    }

    suspend fun addLocalHistoryItem(item: ActivityItem) {
        context.checkFirstDataStore.edit { p ->
            val current = parseHistoryJson(p[Keys.LOCAL_HISTORY_JSON].orEmpty())
            val updated = (listOf(item) + current).take(30)
            p[Keys.LOCAL_HISTORY_JSON] = serializeHistoryJson(updated)
        }
    }

    suspend fun clearLocalHistory() {
        context.checkFirstDataStore.edit { p ->
            p.remove(Keys.LOCAL_HISTORY_JSON)
        }
    }

    private fun parseHistoryJson(raw: String): List<ActivityItem> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        ActivityItem(
                            id = obj.optString("id"),
                            kind = obj.optString("kind"),
                            label = obj.optString("label"),
                            risk = RiskLevel.fromWire(obj.optString("risk")),
                            date = obj.optString("date"),
                            isDemo = false,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeHistoryJson(items: List<ActivityItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("kind", item.kind)
                    put("label", item.label)
                    put("risk", item.risk.wireValue)
                    put("date", item.date)
                },
            )
        }
        return arr.toString()
    }
}

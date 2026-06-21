package com.wyd.mypurse.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好持久化仓库（基于 DataStore Preferences）。
 *
 * 键清单：
 * - [THEME_PRESET_NAME]：当前主题预设名，默认 "默认紫"
 * - [CUSTOM_CHART_PALETTE]：用户自定义图表配色 JSON 数组字符串，空表示跟随主题预设
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** DataStore 文件名 */
        private const val PREFERENCES_NAME = "mypurse_preferences"

        /** 主题预设名（String） */
        val THEME_PRESET_NAME = stringPreferencesKey("theme_preset_name")

        /** 自定义图表配色 JSON（String，空=跟随主题预设） */
        val CUSTOM_CHART_PALETTE = stringPreferencesKey("custom_chart_palette")

        /** 默认预设名 */
        const val DEFAULT_PRESET_NAME = "默认紫"
    }

    // 每个文件只创建一次 DataStore，通过属性委托保证单例
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

    private val dataStore: DataStore<Preferences> = context.dataStore

    /** 当前主题预设名 Flow */
    val themePresetName: Flow<String> = dataStore.data.map { prefs ->
        prefs[THEME_PRESET_NAME] ?: DEFAULT_PRESET_NAME
    }

    /** 自定义图表配色 JSON Flow（空字符串表示跟随主题） */
    val customChartPalette: Flow<String> = dataStore.data.map { prefs ->
        prefs[CUSTOM_CHART_PALETTE] ?: ""
    }

    /** 设置主题预设名 */
    suspend fun setThemePresetName(name: String) {
        dataStore.edit { prefs ->
            prefs[THEME_PRESET_NAME] = name
        }
    }

    /** 设置自定义图表配色（JSON 数组字符串，传空字符串清除自定义） */
    suspend fun setCustomChartPalette(jsonPalette: String) {
        dataStore.edit { prefs ->
            if (jsonPalette.isEmpty()) {
                prefs.remove(CUSTOM_CHART_PALETTE)
            } else {
                prefs[CUSTOM_CHART_PALETTE] = jsonPalette
            }
        }
    }
}

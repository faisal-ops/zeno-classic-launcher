package com.zeno.classiclauncher.nlauncher.locale

import android.annotation.SuppressLint
import android.content.Context
import android.os.LocaleList
import java.util.Locale

data class LauncherLanguage(
    val code: String,
    val title: String,
    val subtitle: String,
)

object LauncherLocale {
    private const val PREFS_NAME = "zeno_locale"
    private const val KEY_LANGUAGE_CODE = "languageCode"

    val supportedLanguages: List<LauncherLanguage> = listOf(
        LauncherLanguage("", "System default", "Use your Android language"),
        LauncherLanguage("en", "English", "Built in"),
        LauncherLanguage("id", "Bahasa Indonesia", "Indonesian"),
        LauncherLanguage("de", "Deutsch", "German"),
        LauncherLanguage("es", "Español", "Spanish"),
        LauncherLanguage("fr", "Français", "French"),
        LauncherLanguage("hi", "हिन्दी", "Hindi"),
        LauncherLanguage("it", "Italiano", "Italian"),
        LauncherLanguage("pt", "Português", "Portuguese"),
        LauncherLanguage("ru", "Русский", "Russian"),
        LauncherLanguage("ja", "日本語", "Japanese"),
        LauncherLanguage("ko", "한국어", "Korean"),
        LauncherLanguage("zh", "中文", "Chinese"),
    )

    fun normalize(languageCode: String): String {
        val trimmed = languageCode.trim()
        return supportedLanguages.firstOrNull { it.code == trimmed }?.code ?: ""
    }

    fun currentLanguageCode(context: Context): String =
        normalize(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANGUAGE_CODE, "") ?: "")

    @SuppressLint("ApplySharedPref")
    fun persist(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_CODE, normalize(languageCode))
            .commit()
    }

    fun languageTitle(languageCode: String): String =
        supportedLanguages.firstOrNull { it.code == normalize(languageCode) }?.title ?: supportedLanguages.first().title

    fun apply(base: Context): Context {
        val languageCode = currentLanguageCode(base)
        if (languageCode.isEmpty()) return base
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val config = base.resources.configuration
        val updatedConfig = android.content.res.Configuration(config)
        updatedConfig.setLocales(LocaleList(locale))
        return base.createConfigurationContext(updatedConfig)
    }
}

package lk.moneytrail.app.data.preferences

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemePreferencesTest {

    private class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getString(key: String?, defValue: String?): String? =
            (map[key] as? String) ?: defValue

        override fun edit(): SharedPreferences.Editor = FakeEditor(map)

        override fun getAll(): MutableMap<String, *> = map
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (map[key] as? MutableSet<String>) ?: defValues
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) map[key] = value
                return this
            }
            override fun apply() {}
            override fun commit(): Boolean = true
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) map.remove(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                map.clear()
                return this
            }
        }
    }

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var fakeContext: Context
    private lateinit var themePreferences: ThemePreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        fakeContext = object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = fakePrefs
        }
        themePreferences = ThemePreferences(fakeContext)
    }

    @Test
    fun themeMode_hasCorrectLabels() {
        assertEquals("Light", ThemeMode.LIGHT.label)
        assertEquals("Dark", ThemeMode.DARK.label)
        assertEquals("System Default", ThemeMode.SYSTEM.label)
    }

    @Test
    fun themeMode_enumEntriesExist() {
        assertEquals(3, ThemeMode.entries.size)
        assertEquals(ThemeMode.LIGHT, ThemeMode.valueOf("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.valueOf("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
    }

    @Test
    fun themePreferences_defaultsToSystemMode() {
        assertEquals(ThemeMode.SYSTEM, themePreferences.getThemeMode())
        assertEquals(ThemeMode.SYSTEM, themePreferences.themeMode.value)
    }

    @Test
    fun themePreferences_savesAndLoadsLightMode() {
        themePreferences.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, themePreferences.getThemeMode())
        assertEquals(ThemeMode.LIGHT, themePreferences.themeMode.value)
        assertEquals("LIGHT", fakePrefs.map["key_theme_mode"])
    }

    @Test
    fun themePreferences_savesAndLoadsDarkMode() {
        themePreferences.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, themePreferences.getThemeMode())
        assertEquals(ThemeMode.DARK, themePreferences.themeMode.value)
        assertEquals("DARK", fakePrefs.map["key_theme_mode"])
    }

    @Test
    fun themePreferences_savesAndLoadsSystemMode() {
        themePreferences.setThemeMode(ThemeMode.DARK)
        themePreferences.setThemeMode(ThemeMode.SYSTEM)

        assertEquals(ThemeMode.SYSTEM, themePreferences.getThemeMode())
        assertEquals(ThemeMode.SYSTEM, themePreferences.themeMode.value)
        assertEquals("SYSTEM", fakePrefs.map["key_theme_mode"])
    }

    @Test
    fun themePreferences_restoresSavedThemeModeOnInitialization() {
        fakePrefs.map["key_theme_mode"] = "DARK"

        val newPreferences = ThemePreferences(fakeContext)

        assertEquals(ThemeMode.DARK, newPreferences.getThemeMode())
        assertEquals(ThemeMode.DARK, newPreferences.themeMode.value)
    }
}

package lk.moneytrail.app.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesTest {

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
}

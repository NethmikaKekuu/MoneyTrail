package lk.moneytrail.app.presentation.settings

import lk.moneytrail.app.data.preferences.ThemeMode
import lk.moneytrail.app.domain.model.CustomCategory

data class SettingsState(
    /** Light, Dark or the phone's own setting. */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Categories the user added, A to Z. */
    val categories: List<CustomCategory> = emptyList(),
    val categoriesError: String? = null,
    /** True while the "New category" dialog is open. */
    val isAddingCategory: Boolean = false,
    val isSavingCategory: Boolean = false,
    val categoryError: String? = null,
    /** The category waiting for the user to confirm it should be deleted. */
    val categoryToDelete: CustomCategory? = null
)

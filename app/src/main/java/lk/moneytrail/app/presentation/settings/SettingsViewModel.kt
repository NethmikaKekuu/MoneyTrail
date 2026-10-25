package lk.moneytrail.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.data.preferences.ThemeMode
import lk.moneytrail.app.data.preferences.ThemePreferences
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.CategoryRepository
import lk.moneytrail.app.domain.usecase.AddCategoryUseCase
import javax.inject.Inject

/** The app theme and the user's own categories. The salary lives on Salary. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val addCategory: AddCategoryUseCase,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { result ->
                result
                    .onSuccess { categories -> _uiState.update { it.copy(categories = categories, categoriesError = null) } }
                    .onFailure { e -> _uiState.update { it.copy(categoriesError = e.message) } }
            }
        }
    }

    /** Saved on the phone, so the app opens in the chosen theme next time too. */
    fun onThemeModeClick(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun onAddCategoryClick() {
        _uiState.update { it.copy(isAddingCategory = true, categoryError = null) }
    }

    fun onAddCategoryDismiss() {
        _uiState.update { it.copy(isAddingCategory = false, categoryError = null) }
    }

    fun onAddCategory(name: String, type: EntryType, spendingType: SpendingType?) {
        val state = _uiState.value
        if (state.isSavingCategory) return

        _uiState.update { it.copy(isSavingCategory = true, categoryError = null) }
        viewModelScope.launch {
            addCategory(name, type, spendingType, state.categories)
                .onSuccess { _uiState.update { it.copy(isSavingCategory = false, isAddingCategory = false) } }
                .onFailure { e -> _uiState.update { it.copy(isSavingCategory = false, categoryError = e.message) } }
        }
    }

    fun onDeleteCategoryClick(id: String) {
        _uiState.update { state -> state.copy(categoryToDelete = state.categories.find { it.id == id }, categoriesError = null) }
    }

    fun onDeleteCategoryDismiss() {
        _uiState.update { it.copy(categoryToDelete = null) }
    }

    fun onDeleteCategoryConfirm() {
        val category = _uiState.value.categoryToDelete ?: return
        _uiState.update { it.copy(categoryToDelete = null) }
        viewModelScope.launch {
            categoryRepository.deleteCategory(category.id)
                .onFailure { e -> _uiState.update { it.copy(categoriesError = e.message) } }
        }
    }
}

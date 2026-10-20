package lk.moneytrail.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.moneytrail.app.domain.model.EntryType
import lk.moneytrail.app.domain.model.SpendingType
import lk.moneytrail.app.domain.repository.CategoryRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.usecase.AddCategoryUseCase
import lk.moneytrail.app.domain.usecase.UpdateSalaryUseCase
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val categoryRepository: CategoryRepository,
    private val updateSalary: UpdateSalaryUseCase,
    private val addCategory: AddCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        loadPlan()
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { result ->
                result
                    .onSuccess { categories -> _uiState.update { it.copy(categories = categories, categoriesError = null) } }
                    .onFailure { e -> _uiState.update { it.copy(categoriesError = e.message) } }
            }
        }
    }

    fun loadPlan() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            planRepository.getPlan()
                .onSuccess { plan ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasPlan = plan != null,
                            payDay = plan?.payDay,
                            baseSalaryDigits = plan?.baseSalary?.toString().orEmpty()
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, loadError = e.message ?: "Couldn't load your settings.") }
                }
        }
    }

    fun onPayDayClick(day: Int) {
        _uiState.update { it.copy(payDay = day, salaryError = null, isSalarySaved = false) }
    }

    fun onBaseSalaryChange(text: String) {
        val digits = text.filter(Char::isDigit).take(MAX_DIGITS)
        _uiState.update { it.copy(baseSalaryDigits = digits, salaryError = null, isSalarySaved = false) }
    }

    fun onSaveSalaryClick() {
        val state = _uiState.value
        if (state.isSavingSalary) return

        _uiState.update { it.copy(isSavingSalary = true, salaryError = null, isSalarySaved = false) }
        viewModelScope.launch {
            updateSalary(state.payDay, state.baseSalary, LocalDate.now())
                .onSuccess { _uiState.update { it.copy(isSavingSalary = false, isSalarySaved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSavingSalary = false, salaryError = e.message) } }
        }
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

    private companion object {
        /** LKR 999,999,999 at most, as everywhere else in the app. */
        const val MAX_DIGITS = 9
    }
}

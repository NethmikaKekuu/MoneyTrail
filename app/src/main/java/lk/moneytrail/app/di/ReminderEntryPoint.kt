package lk.moneytrail.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.NotificationLogRepository
import lk.moneytrail.app.domain.usecase.GetRemindersUseCase

/**
 * How the background reminder check gets what it needs from Hilt. WorkManager makes the
 * worker itself, so it can't take them in its constructor like a ViewModel.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun getReminders(): GetRemindersUseCase
    fun authRepository(): AuthRepository
    fun notificationLog(): NotificationLogRepository
}

package lk.moneytrail.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lk.moneytrail.app.data.preferences.CatchUpPreferences
import lk.moneytrail.app.data.reminders.WorkManagerReminderScheduler
import lk.moneytrail.app.data.notifications.RoomNotificationLogRepository
import lk.moneytrail.app.data.repository.AndroidConnectivityRepository
import lk.moneytrail.app.data.repository.FirebaseAuthRepository
import lk.moneytrail.app.data.repository.FirestoreAccountRepository
import lk.moneytrail.app.data.repository.FirestoreCategoryRepository
import lk.moneytrail.app.data.repository.FirestoreEntryRepository
import lk.moneytrail.app.data.repository.FirestoreFreelanceRepository
import lk.moneytrail.app.data.repository.FirestorePlanRepository
import lk.moneytrail.app.data.repository.FirestoreUserRepository
import lk.moneytrail.app.data.repository.HttpUsdRateRepository
import lk.moneytrail.app.domain.repository.AccountRepository
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.CatchUpRepository
import lk.moneytrail.app.domain.repository.CategoryRepository
import lk.moneytrail.app.domain.repository.ConnectivityRepository
import lk.moneytrail.app.domain.repository.EntryRepository
import lk.moneytrail.app.domain.repository.FreelanceRepository
import lk.moneytrail.app.domain.repository.NotificationLogRepository
import lk.moneytrail.app.domain.repository.PlanRepository
import lk.moneytrail.app.domain.repository.ReminderScheduler
import lk.moneytrail.app.domain.repository.UsdRateRepository
import lk.moneytrail.app.domain.repository.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirestoreUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindEntryRepository(impl: FirestoreEntryRepository): EntryRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: FirestoreAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: FirestorePlanRepository): PlanRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: FirestoreCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindNotificationLogRepository(impl: RoomNotificationLogRepository): NotificationLogRepository

    @Binds
    @Singleton
    abstract fun bindUsdRateRepository(impl: HttpUsdRateRepository): UsdRateRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityRepository(impl: AndroidConnectivityRepository): ConnectivityRepository

    @Binds
    @Singleton
    abstract fun bindCatchUpRepository(impl: CatchUpPreferences): CatchUpRepository

    @Binds
    @Singleton
    abstract fun bindFreelanceRepository(impl: FirestoreFreelanceRepository): FreelanceRepository

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}

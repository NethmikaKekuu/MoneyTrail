package lk.moneytrail.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lk.moneytrail.app.data.repository.FirebaseAuthRepository
import lk.moneytrail.app.data.repository.FirestoreEntryRepository
import lk.moneytrail.app.data.repository.FirestoreUserRepository
import lk.moneytrail.app.domain.repository.AuthRepository
import lk.moneytrail.app.domain.repository.EntryRepository
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
}

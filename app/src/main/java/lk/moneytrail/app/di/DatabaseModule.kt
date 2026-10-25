package lk.moneytrail.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import lk.moneytrail.app.data.local.EntryDao
import lk.moneytrail.app.data.local.MoneyTrailDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyTrailDatabase =
        Room.databaseBuilder(context, MoneyTrailDatabase::class.java, "moneytrail.db")
            // Room only holds a copy of Firestore, so after a schema change it's safe
            // to drop the tables and let the listeners fill them again.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideEntryDao(database: MoneyTrailDatabase): EntryDao = database.entryDao()
}

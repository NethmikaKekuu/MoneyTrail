package lk.moneytrail.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import lk.moneytrail.app.data.notifications.NotificationDao
import lk.moneytrail.app.data.notifications.NotificationDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationLogModule {

    @Provides
    @Singleton
    fun provideNotificationDatabase(@ApplicationContext context: Context): NotificationDatabase =
        // No destructive fallback: this holds the user's own list, not a copy of Firestore.
        Room.databaseBuilder(context, NotificationDatabase::class.java, "moneytrail_notifications.db").build()

    @Provides
    fun provideNotificationDao(database: NotificationDatabase): NotificationDao = database.notificationDao()
}

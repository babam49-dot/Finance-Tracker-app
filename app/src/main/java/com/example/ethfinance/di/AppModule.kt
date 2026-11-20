package com.example.ethfinance.di

import android.content.Context
import androidx.room.Room
import com.example.ethfinance.core.common.DefaultDispatcherProvider
import com.example.ethfinance.core.common.DispatcherProvider
import com.example.ethfinance.data.local.db.AppDatabase
import com.example.ethfinance.data.local.db.dao.AccountDao
import com.example.ethfinance.data.local.db.dao.BudgetDao
import com.example.ethfinance.data.local.db.dao.CategoryDao
import com.example.ethfinance.data.local.db.dao.SyncStateDao
import com.example.ethfinance.data.local.db.dao.TransactionDao
import com.example.ethfinance.data.local.db.dao.UserRuleDao
import com.example.ethfinance.data.local.prefs.SettingsDataStore
import com.example.ethfinance.data.repository.AccountRepositoryImpl
import com.example.ethfinance.data.repository.ReportRepositoryImpl
import com.example.ethfinance.data.repository.SettingsRepositoryImpl
import com.example.ethfinance.data.repository.TransactionRepositoryImpl
import com.example.ethfinance.domain.repository.AccountRepository
import com.example.ethfinance.domain.repository.ReportRepository
import com.example.ethfinance.domain.repository.SettingsRepository
import com.example.ethfinance.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideUserRuleDao(db: AppDatabase): UserRuleDao = db.userRuleDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideSyncStateDao(db: AppDatabase): SyncStateDao = db.syncStateDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

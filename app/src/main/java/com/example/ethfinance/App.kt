package com.example.ethfinance

import android.app.Application
import com.example.ethfinance.core.common.DispatcherProvider
import com.example.ethfinance.core.util.NotificationHelper
import com.example.ethfinance.data.demo.DemoSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var demoSeeder: DemoSeeder

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannels()

        applicationScope.launch(dispatcherProvider.io) {
            demoSeeder.seedIfNeeded()
        }
    }
}

package com.example.ethfinance.core.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ethfinance.MainActivity
import com.example.ethfinance.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_TRANSACTIONS = "channel_transactions"
        const val CHANNEL_SYNC = "channel_sync"
        private const val NOTIFICATION_SYNC_ID = 1001
        private const val NOTIFICATION_TX_BASE_ID = 2000
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val txChannel = NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Transactions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a new transaction is parsed from bank SMS"
            }

            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Bank Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when bank messages are auto-synced"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(txChannel)
            manager.createNotificationChannel(syncChannel)
        }
    }

    fun notifyNewTransaction(
        title: String,
        message: String,
        txId: Long
    ) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            txId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify((NOTIFICATION_TX_BASE_ID + (txId % 1000)).toInt(), notification)
    }

    fun notifySyncCompleted(importedCount: Int) {
        if (!hasNotificationPermission()) return

        val message = if (importedCount > 0) {
            "Auto-synced: $importedCount new bank message(s) imported from CBE & Telebirr."
        } else {
            "Auto-synced: Your bank accounts and balances are up to date."
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ethiopian Finance Tracker")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_SYNC_ID, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

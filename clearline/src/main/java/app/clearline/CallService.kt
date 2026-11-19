package app.clearline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Keeps an ongoing-call notification while Clearline is in an active session.
 */
class CallService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Clearline"
                val text = intent?.getStringExtra(EXTRA_TEXT) ?: "Call in progress"
                ensureChannel()
                startForeground(NOTIF_ID, buildNotification(title, text))
            }
        }
        return START_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_ID, "Calls", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Ongoing Clearline calls"
        mgr.createNotificationChannel(ch)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "clearline_calls"
        const val NOTIF_ID = 42
        const val ACTION_START = "app.clearline.CALL_START"
        const val ACTION_STOP = "app.clearline.CALL_STOP"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"

        fun start(ctx: Context, title: String, text: String) {
            val i = Intent(ctx, CallService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TEXT, text)
            }
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, CallService::class.java).apply { action = ACTION_STOP })
        }
    }
}

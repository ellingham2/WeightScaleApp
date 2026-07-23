package com.matth.scaleconnect.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.matth.scaleconnect.MainActivity
import com.matth.scaleconnect.ble.ConnectionState
import com.matth.scaleconnect.ble.ScaleBleManager
import com.matth.scaleconnect.data.ProfileRepository
import com.matth.scaleconnect.data.ScaleDatabase
import com.matth.scaleconnect.data.WeighInRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Keeps a BLE connection to the scale alive in the background: reconnects whenever
 * disconnected, syncs the active profile once ready, and persists completed weigh-ins
 * to Room - all independent of whether the app UI is open. See
 * ScaleViewModel for the equivalent read-only UI-side observation of the same
 * ScaleBleManager singleton.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ScaleConnectionService : LifecycleService() {

    private lateinit var bleManager: ScaleBleManager
    private lateinit var db: ScaleDatabase
    private lateinit var profileRepo: ProfileRepository

    override fun onCreate() {
        super.onCreate()
        bleManager = ScaleBleManager.getInstance(this)
        db = ScaleDatabase.getInstance(this)
        profileRepo = ProfileRepository(applicationContext)

        createNotificationChannel()
        val notification = buildNotification("Watching for scale…")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            },
        )

        // Reconnect loop: fires immediately for the current state too, so this also
        // covers the very first scan when the service starts.
        bleManager.connectionState.onEach { state ->
            updateNotification(state)
            if (state == ConnectionState.DISCONNECTED) {
                delay(2000)
                bleManager.startScan()
            }
        }.launchIn(lifecycleScope)

        // The scale needs the active profile synced once per connection before its
        // firmware will compute correct per-user body-composition metrics.
        bleManager.connectionState.onEach { state ->
            if (state == ConnectionState.READY) syncActiveProfile()
        }.launchIn(lifecycleScope)

        // Once a weigh-in settles (all metrics populated and unchanged for a beat),
        // persist it once for the currently active profile slot.
        combine(
            bleManager.reading.debounce(1500),
            profileRepo.activeSlotId,
        ) { r, slotId -> r to slotId }
            .filter { (r, _) ->
                r.weightLb != null && r.bodyFatPercent != null &&
                    r.hydrationPercent != null && r.musclePercent != null &&
                    r.bonePercent != null && r.bmrKcal != null
            }
            .distinctUntilChanged()
            .onEach { (r, slotId) ->
                val latest = db.weighInDao().getLatestForSlot(slotId)
                val isSame = latest != null &&
                    latest.weightLb == r.weightLb && latest.bmrKcal == r.bmrKcal
                if (!isSame) {
                    db.weighInDao().insert(
                        WeighInRecord(
                            slotId = slotId,
                            timestampMillis = System.currentTimeMillis(),
                            weightLb = r.weightLb!!,
                            bodyFatPercent = r.bodyFatPercent!!,
                            hydrationPercent = r.hydrationPercent!!,
                            musclePercent = r.musclePercent!!,
                            bonePercent = r.bonePercent!!,
                            bmrKcal = r.bmrKcal!!,
                        )
                    )
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun syncActiveProfile() {
        lifecycleScope.launch {
            val activeId = profileRepo.activeSlotId.first()
            val slot = profileRepo.profileSlots.first().find { it.id == activeId } ?: return@launch
            bleManager.sendProfileSync(
                userIndex = slot.id,
                isMale = slot.isMale,
                age = slot.age,
                heightCm = slot.heightCm,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.disconnect()
    }

    override fun onBind(intent: Intent): android.os.IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Scale connection",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when ScaleConnect is watching for or connected to your scale"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScaleConnect")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(state: ConnectionState) {
        val text = when (state) {
            ConnectionState.DISCONNECTED -> "Watching for scale…"
            ConnectionState.SCANNING -> "Scanning…"
            ConnectionState.CONNECTING, ConnectionState.DISCOVERING -> "Connecting…"
            ConnectionState.READY -> "Connected · ${bleManager.deviceName.value ?: "Scale"}"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "scale_connection"
        private const val NOTIFICATION_ID = 1
    }
}

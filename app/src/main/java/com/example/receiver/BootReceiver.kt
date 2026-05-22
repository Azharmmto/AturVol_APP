package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.AppSettings
import com.example.service.VolumeOverlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settings = AppSettings(context)
            if (settings.getSidebarEnabled() && settings.getStartupAtBoot()) {
                val serviceIntent = Intent(context, VolumeOverlayService::class.java).apply {
                    action = VolumeOverlayService.ACTION_START_SERVICE
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            settings.cleanup()
        }
    }
}

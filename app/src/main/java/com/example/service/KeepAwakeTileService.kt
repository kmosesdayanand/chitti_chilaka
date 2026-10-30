package com.example.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.example.R
import com.example.data.KeepAwakeDuration
import com.example.data.KeepAwakeManager

class KeepAwakeTileService : TileService() {

    companion object {
        private const val TAG = "KeepAwakeTileService"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        // Verify if service is actually active
        val isCurrentlyActive = KeepAwakeService.isServiceRunning && KeepAwakeManager.isRunning
        val currentDuration = if (isCurrentlyActive) {
            KeepAwakeManager.currentDuration
        } else {
            KeepAwakeDuration.OFF
        }

        val nextDuration = currentDuration.next()
        Log.d(TAG, "Tile clicked: transitioning from ${currentDuration.label} to ${nextDuration.label}")

        if (nextDuration == KeepAwakeDuration.OFF) {
            KeepAwakeManager.stop(applicationContext)
        } else {
            KeepAwakeManager.start(applicationContext, nextDuration)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        // Verify real running state to avoid being stuck on Active if service was killed
        val isActive = KeepAwakeService.isServiceRunning && KeepAwakeManager.isRunning
        val duration = if (isActive) KeepAwakeManager.currentDuration else KeepAwakeDuration.OFF

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(
            this,
            if (isActive) R.drawable.ic_keep_awake_on else R.drawable.ic_keep_awake_off
        )
        tile.label = getString(R.string.keep_awake_tile_label)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isActive) {
                KeepAwakeManager.getRemainingSubtitle()
            } else {
                "Off"
            }
        }

        tile.updateTile()
    }
}

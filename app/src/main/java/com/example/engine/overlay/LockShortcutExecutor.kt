package com.example.engine.overlay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.Immutable
import com.example.data.model.LockShortcutAction
import com.example.data.model.LockShortcutConfig

@Immutable
data class LaunchableAppInfo(
    val packageName: String,
    val label: String
)

data class ShortcutExecutionOutcome(
    val executed: Boolean,
    val message: String = "",
    val launchIntent: Intent? = null
)

/**
 * Executes real Android lock-screen corner shortcuts (Camera2 torch, Clock intents,
 * Camera capture, Calculator, System Settings, and explicit Open App picker targets).
 */
object LockShortcutExecutor {

    @Volatile
    private var torchCurrentlyOn: Boolean = false

    fun isTorchOn(): Boolean = torchCurrentlyOn

    fun registerTorchObserver(
        context: Context,
        onStateChanged: (Boolean) -> Unit
    ): () -> Unit {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return {}
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchCurrentlyOn = enabled
                onStateChanged(enabled)
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                torchCurrentlyOn = false
                onStateChanged(false)
            }
        }
        runCatching {
            cm.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        }
        return {
            runCatching { cm.unregisterTorchCallback(callback) }
        }
    }

    fun toggleFlashlight(context: Context): ShortcutExecutionOutcome {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ShortcutExecutionOutcome(false, "Flashlight service unavailable")
        val cameraId = runCatching {
            cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val isBack = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                hasFlash && isBack
            } ?: cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()

        if (cameraId == null) {
            return ShortcutExecutionOutcome(false, "No camera flash hardware available")
        }

        val next = !torchCurrentlyOn
        return runCatching {
            cm.setTorchMode(cameraId, next)
            torchCurrentlyOn = next
            ShortcutExecutionOutcome(true, if (next) "Flashlight on" else "Flashlight off")
        }.getOrElse {
            ShortcutExecutionOutcome(false, "Unable to toggle flashlight")
        }
    }

    fun buildLaunchIntent(context: Context, config: LockShortcutConfig): Intent? {
        val pm = context.packageManager
        val intent = when (config.action) {
            LockShortcutAction.NONE,
            LockShortcutAction.FLASHLIGHT -> null

            LockShortcutAction.CAMERA -> {
                val still = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                if (still.resolveActivity(pm) != null) still
                else Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            }

            LockShortcutAction.QR_SCANNER -> {
                val still = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                if (still.resolveActivity(pm) != null) still
                else Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            }

            LockShortcutAction.ALARM -> {
                val show = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                if (show.resolveActivity(pm) != null) show
                else Intent(AlarmClock.ACTION_SET_ALARM)
            }

            LockShortcutAction.TIMER -> Intent(AlarmClock.ACTION_SET_TIMER)

            LockShortcutAction.STOPWATCH -> {
                val timers = Intent(AlarmClock.ACTION_SHOW_TIMERS)
                if (timers.resolveActivity(pm) != null) timers
                else Intent(AlarmClock.ACTION_SET_TIMER)
            }

            LockShortcutAction.CALCULATOR -> {
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)
            }

            LockShortcutAction.SETTINGS -> Intent(Settings.ACTION_SETTINGS)

            LockShortcutAction.OPEN_APP -> {
                if (config.packageName.isBlank()) null
                else pm.getLaunchIntentForPackage(config.packageName)
            }
        } ?: return null

        return intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    fun executeShortcut(
        context: Context,
        config: LockShortcutConfig,
        onBeforeLaunchExternalActivity: ((Intent) -> Boolean)? = null
    ): ShortcutExecutionOutcome {
        return when (config.action) {
            LockShortcutAction.NONE -> ShortcutExecutionOutcome(false, "")
            LockShortcutAction.FLASHLIGHT -> toggleFlashlight(context)
            else -> {
                val intent = buildLaunchIntent(context, config)
                    ?: return ShortcutExecutionOutcome(false, "${config.displayLabel} is not available on this device")
                if (onBeforeLaunchExternalActivity != null && !onBeforeLaunchExternalActivity(intent)) {
                    return ShortcutExecutionOutcome(true, "Unlock to open ${config.displayLabel}", launchIntent = intent)
                }
                val started = runCatching {
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (started) {
                    ShortcutExecutionOutcome(true, "Opening ${config.displayLabel}", launchIntent = intent)
                } else {
                    ShortcutExecutionOutcome(false, "No app installed for ${config.displayLabel}")
                }
            }
        }
    }

    fun queryLaunchableApps(context: Context): List<LaunchableAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        }.getOrElse { emptyList() }
        val selfPkg = context.packageName
        return resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == selfPkg) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                LaunchableAppInfo(packageName = pkg, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}

package com.example

import android.app.Application
import com.example.engine.crash.ZeroCrashEngine
import com.example.engine.performance.ZeroLagPerformanceEngine

/**
 * Custom Application class initializing Zero Crash Engine V1 and
 * Zero-Lag & Preloading Performance Engine V4 at process launch.
 */
class BackdropApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ZeroCrashEngine.init(this)
        ZeroLagPerformanceEngine.init(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        ZeroLagPerformanceEngine.handleTrimMemory(this, level)
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_COMPLETE) {
            ZeroCrashEngine.triggerEmergencyMemoryCleanup()
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onLowMemory() {
        super.onLowMemory()
        ZeroLagPerformanceEngine.handleTrimMemory(this, TRIM_MEMORY_COMPLETE)
        ZeroCrashEngine.triggerEmergencyMemoryCleanup()
    }
}

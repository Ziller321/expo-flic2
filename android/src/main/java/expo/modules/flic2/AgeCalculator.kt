package expo.modules.flic2

/**
 * Translates a Flic2 button event timestamp into an age (ms since the physical press).
 *
 * The Flic2 SDK provides timestamps in milliseconds since the *button's own boot*, which is
 * unrelated to Android's clocks. To convert, we establish a correlation at onReady time:
 * we record both the button's readyTimestamp and Android's SystemClock.elapsedRealtime()
 * at the same moment, then use that offset for all subsequent events.
 */
object AgeCalculator {
    /**
     * @param nowElapsedMs          SystemClock.elapsedRealtime() at the moment of processing
     * @param androidReadyElapsedMs SystemClock.elapsedRealtime() recorded when onReady fired
     * @param buttonReadyTimestamp  button's readyTimestamp (ms since button boot) from onReady
     * @param eventTimestamp        button's event timestamp (ms since button boot)
     * @return age in milliseconds, clamped to 0 (never negative)
     */
    fun computeAgeMs(
        nowElapsedMs: Long,
        androidReadyElapsedMs: Long,
        buttonReadyTimestamp: Long,
        eventTimestamp: Long
    ): Long {
        val estimatedAndroidEventTime = androidReadyElapsedMs + (eventTimestamp - buttonReadyTimestamp)
        return maxOf(0L, nowElapsedMs - estimatedAndroidEventTime)
    }
}

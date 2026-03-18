package expo.modules.flic2

import org.junit.Assert.assertEquals
import org.junit.Test

class AgeCalculatorTest {

    // Scenario: button has been running for 10_000ms since its own boot.
    // Android has been running for 5_000ms since its own boot.
    // onReady fired when button reported 10_000ms and Android was at 5_000ms.

    private val androidReady = 5_000L
    private val buttonReady = 10_000L

    @Test
    fun `fresh press arrives with near-zero latency`() {
        // Button pressed at button-time 10_050ms (50ms after ready).
        // Android receives the event at android-time 5_080ms (80ms after ready).
        // Expected age = 80 - 50 = 30ms.
        val age = AgeCalculator.computeAgeMs(
            nowElapsedMs = 5_080L,
            androidReadyElapsedMs = androidReady,
            buttonReadyTimestamp = buttonReady,
            eventTimestamp = 10_050L
        )
        assertEquals(30L, age)
    }

    @Test
    fun `queued press that happened 5 seconds before button connected`() {
        // Button pressed at button-time 5_000ms (5_000ms before ready at 10_000ms).
        // Android receives at android-time 5_100ms (100ms after ready).
        // Expected age = 5_100 - (5_000 + (5_000 - 10_000)) = 5_100 - 0 = 5_100ms.
        val age = AgeCalculator.computeAgeMs(
            nowElapsedMs = 5_100L,
            androidReadyElapsedMs = androidReady,
            buttonReadyTimestamp = buttonReady,
            eventTimestamp = 5_000L
        )
        assertEquals(5_100L, age)
    }

    @Test
    fun `age is clamped to zero when button clock is slightly ahead of android clock`() {
        // eventTimestamp slightly ahead of ready (clock drift), should not go negative.
        val age = AgeCalculator.computeAgeMs(
            nowElapsedMs = 5_000L,
            androidReadyElapsedMs = androidReady,
            buttonReadyTimestamp = buttonReady,
            eventTimestamp = 10_020L  // 20ms into the future relative to now
        )
        assertEquals(0L, age)
    }

    @Test
    fun `age is zero when event happens exactly now`() {
        // Button pressed at button-time 10_200ms (200ms after ready).
        // Android receives at android-time 5_200ms (200ms after ready).
        // Perfectly synchronised clocks => age = 0.
        val age = AgeCalculator.computeAgeMs(
            nowElapsedMs = 5_200L,
            androidReadyElapsedMs = androidReady,
            buttonReadyTimestamp = buttonReady,
            eventTimestamp = 10_200L
        )
        assertEquals(0L, age)
    }
}

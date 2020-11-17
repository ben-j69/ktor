/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.util

import kotlinx.coroutines.*
import kotlin.test.*
import org.junit.Test

class StartTimeoutTest {

    private data class TestClock(var timeMs: Long)

    private val timeoutMs: Long = 100
    private val clock = TestClock(2000)

    @Test
    fun testTimeoutInvocation() = runBlocking {
        var timeoutInvoked = false

        val timeout = startTimeout("test", timeoutMs, clock::timeMs) { timeoutInvoked = true }
        yield()

        clock.timeMs += timeoutMs
        delay(timeoutMs)
        yield()
        assertTrue(timeoutInvoked)
    }

    @Test
    fun testTimeoutCancellation() = runBlocking {
        var timeoutInvoked = false

        val timeout = startTimeout("test", timeoutMs, clock::timeMs) { timeoutInvoked = true }
        yield()

        clock.timeMs += timeoutMs
        timeout.cancel()
        delay(timeoutMs)
        yield()
        assertFalse(timeoutInvoked)
    }

    @Test
    fun testTimeoutUpdateActivityTime() = runBlocking {
        var timeoutInvoked = false
        val timeout = startTimeout("test", timeoutMs, clock::timeMs) { timeoutInvoked = true }
        yield()

        clock.timeMs += timeoutMs
        timeout.lastActivityTime = clock.timeMs - timeoutMs / 2
        delay(timeoutMs)
        yield()
        assertFalse(timeoutInvoked)

        clock.timeMs += timeoutMs
        timeout.lastActivityTime = clock.timeMs - timeoutMs / 2
        delay(timeoutMs)
        yield()
        assertFalse(timeoutInvoked)

        clock.timeMs += timeoutMs
        delay(timeoutMs)
        yield()
        assertTrue(timeoutInvoked)
    }

    @Test
    fun testTimeoutCancelsWhenParentScopeCancels() = runBlocking {
        var timeoutInvoked = false
        val scope = CoroutineScope(GlobalScope.coroutineContext)
        val timeout = scope.startTimeout("test", timeoutMs, clock::timeMs) { timeoutInvoked = true }
        yield()

        clock.timeMs += timeoutMs
        runCatching { scope.cancel(CancellationException()) }
        delay(timeoutMs)
        yield()
        assertFalse(timeoutInvoked)
    }
}

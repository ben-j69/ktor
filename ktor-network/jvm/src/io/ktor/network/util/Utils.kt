/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.util

import io.ktor.util.date.*
import io.ktor.utils.io.concurrent.*
import kotlinx.coroutines.*

/**
 * Infinite timeout in milliseconds.
 */
internal const val INFINITE_TIMEOUT_MS = Long.MAX_VALUE

internal class Timeout(private val timeoutJob: Job) {
    internal var lastActivityTime: Long by shared(0)
    fun cancel(): Unit = timeoutJob.cancel()
}

/**
 * Starts timeout coroutine that will invoke [onTimeout] after [timeoutMs] of inactivity.
 * Use [Timeout] object to update last activity time or cancel this coroutine
 */
internal fun CoroutineScope.startTimeout(
    name: String,
    timeoutMs: Long,
    clock: () -> Long = { getTimeMillis() },
    onTimeout: suspend () -> Unit
): Timeout {
    val timoutJob = Job(coroutineContext[Job])
    val timeout = Timeout(timoutJob).apply { lastActivityTime = clock() }
    if (timeoutMs == INFINITE_TIMEOUT_MS) return timeout

    launch(coroutineContext + CoroutineName("Timeout $name") + timoutJob) {
        try {
            while (true) {
                val remaining = timeout.lastActivityTime + timeoutMs - clock()
                if (remaining <= 0) {
                    break
                }

                delay(remaining)
            }
            yield()
            onTimeout()
        } catch (cause: Throwable) {
            // no op
        }
    }.invokeOnCompletion { timoutJob.cancel() }
    return timeout
}

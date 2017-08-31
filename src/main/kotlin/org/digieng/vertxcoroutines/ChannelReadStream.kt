package org.digieng.vertxcoroutines

import io.vertx.core.Context
import io.vertx.core.streams.ReadStream
import kotlinx.coroutines.experimental.channels.ArrayChannel
import kotlinx.coroutines.experimental.channels.Closed
import kotlinx.coroutines.experimental.channels.OFFER_SUCCESS
import kotlinx.coroutines.experimental.channels.POLL_FAILED

internal class ChannelReadStream<T>(
    val context: Context,
    val stream: ReadStream<T>,
    capacity: Int
) : ArrayChannel<T>(capacity) {
    @Volatile
    private var size = 0

    fun subscribe() {
        stream.endHandler { _ -> close() }
        stream.exceptionHandler { close(it) }
        stream.handler { event ->
            context.runCoroutine { send(event) }
        }
    }

    override fun offerInternal(element: T): Any {
        val ret = super.offerInternal(element)

        if (ret == OFFER_SUCCESS) {
            size++
            if (isFull) stream.pause()
        }
        return ret
    }

    override fun pollInternal(): Any? {
        val ret = super.pollInternal()

        if (ret != POLL_FAILED && ret !is Closed<*>) {
            if (--size < capacity / 2) stream.resume()
        }
        return ret
    }
}
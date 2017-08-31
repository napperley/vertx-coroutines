package org.digieng.vertxcoroutines

import io.vertx.core.Context
import io.vertx.core.streams.WriteStream
import kotlinx.coroutines.experimental.channels.ArrayChannel

internal class ChannelWriteStream<T>(
    val context: Context,
    val stream: WriteStream<T>,
    capacity: Int
) : ArrayChannel<T>(capacity) {
    fun subscribe() {
        context.runCoroutine {
            while (true) {
                val elt = receiveOrNull()

                if (stream.writeQueueFull()) {
                    stream.drainHandler { _ -> if (dispatch(elt)) subscribe() }
                    break
                } else {
                    if (!dispatch(elt)) break
                }
            }
        }
    }

    fun dispatch(elt: T?) = if (elt != null) {
        stream.write(elt)
        true
    } else {
        stream.end()
        false
    }
}
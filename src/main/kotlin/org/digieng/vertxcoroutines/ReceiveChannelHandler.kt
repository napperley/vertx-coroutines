package org.digieng.vertxcoroutines

import io.vertx.core.Context
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.streams.ReadStream
import kotlinx.coroutines.experimental.channels.ChannelIterator
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.selects.SelectInstance

/**
 * An adapter that converts a stream of events from the [Handler] into a [ReceiveChannel] which allows the events
 * to be received synchronously. Based on stream's work.
 * @author Nick Apperley
 */
class ReceiveChannelHandler<T> constructor(context: Context) : ReceiveChannel<T>, Handler<T> {

    @Suppress("UsePropertyAccessSyntax")
    constructor(vertx: Vertx) : this(vertx.getOrCreateContext())

    private val stream: ReadStream<T> = object : ReadStream<T> {
        override fun pause(): ReadStream<T> = this
        override fun exceptionHandler(handler: Handler<Throwable>?): ReadStream<T> = this
        override fun endHandler(endHandler: Handler<Void>?): ReadStream<T> = this
        override fun resume(): ReadStream<T> = this
        override fun handler(h: Handler<T>?): ReadStream<T> {
            handler = h
            return this
        }
    }

    private val channel: ReceiveChannel<T> = toChannel(context, stream)
    private var handler: Handler<T>? = null

    override val isClosedForReceive: Boolean
        get() = channel.isClosedForReceive

    override val isEmpty: Boolean
        get() = channel.isEmpty

    override fun iterator(): ChannelIterator<T> = channel.iterator()

    override fun poll(): T? = channel.poll()

    suspend override fun receive(): T = channel.receive()

    suspend override fun receiveOrNull(): T? = channel.receiveOrNull()

    override fun <R> registerSelectReceive(select: SelectInstance<R>, block: suspend (T) -> R) {
        return channel.registerSelectReceive(select, block)
    }

    override fun <R> registerSelectReceiveOrNull(select: SelectInstance<R>, block: suspend (T?) -> R) {
        return channel.registerSelectReceiveOrNull(select, block)
    }

    override fun handle(event: T) {
        val h = handler

        h?.handle(event)
    }
}
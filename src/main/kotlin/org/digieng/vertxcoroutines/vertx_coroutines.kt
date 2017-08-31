package org.digieng.vertxcoroutines

import io.vertx.core.*
import io.vertx.core.Future
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.*
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Based on the work created by stream: https://github.com/vert-x3/vertx-lang-kotlin/blob/
 *     stream1984-kotlin-coroutine-init/vertx-lang-kotlin-coroutine/src/main/kotlin/io/vertx/kotlin/coroutines/
 *     VertxCoroutine.kt).
 * @author Nick Apperley
 */

/**
 * Convert a standard handler to a handler which runs on a coroutine.
 * This is necessary if you want to do fiber blocking synchronous operation in your handler
 */
@Suppress("unused")
fun Vertx.runCoroutine(block: suspend CoroutineScope.() -> Unit) {
    @Suppress("UsePropertyAccessSyntax")
    getOrCreateContext().runCoroutine(block)
}

/**
 * Create a `ReceiveChannelHandler` of some type `T`.
 */
@Suppress("unused")
fun <T> Vertx.receiveChannelHandler(): ReceiveChannelHandler<T> = ReceiveChannelHandler(this)

/**
 * Receive a single event from a handler synchronously.
 * The coroutine will be blocked until the event occurs, this action do not block vertx's eventLoop.
 */
@Suppress("unused")
suspend fun <T> asyncEvent(block: (h: Handler<T>) -> Unit): T = asyncResult { f ->
    val fut = Future.future<T>().setHandler(f)
    val adapter: Handler<T> = Handler { t -> fut.tryComplete(t) }

    try {
        block.invoke(adapter)
    } catch (t: Throwable) {
        fut.tryFail(t)
    }
}

/**
 * Invoke an asynchronous operation and obtain the result synchronous.
 * The coroutine will be blocked until the event occurs, this action do not block vertx's eventLoop.
 */
suspend fun <T> asyncResult(block: (h: Handler<AsyncResult<T>>) -> Unit): T =
    suspendCancellableCoroutine { cont: Continuation<T> ->
        block(Handler { asyncResult ->
            if (asyncResult.succeeded()) cont.resume(asyncResult.result())
            else cont.resumeWithException(asyncResult.cause())
        })
    }

/**
 * Awaits for completion of future without blocking eventLoop
 */
@Suppress("unused")
suspend fun <T> Future<T>.await(): T = when {
    succeeded() -> result()
    failed() -> throw cause()
    else -> suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        setHandler { asyncResult ->
            if (asyncResult.succeeded()) cont.resume(asyncResult.result() as T)
            else cont.resumeWithException(asyncResult.cause())
        }
    }
}

/**
 * Converts this deferred value to the instance of Future.
 * The deferred value is cancelled when the resulting future is cancelled or otherwise completed.
 */
@Suppress("unused")
fun <T> Deferred<T>.asFuture(): Future<T> {
    val future = Future.future<T>()

    future.setHandler({ asyncResult ->
        // if fail, we cancel this job
        if (asyncResult.failed()) cancel(asyncResult.cause())
    })
    invokeOnCompletion {
        try {
            future.complete(getCompleted())
        } catch (t: Throwable) {
            future.fail(VertxException(t))
        }
    }
    return future
}

@Suppress("unused")
fun <T> toChannel(vertx: Vertx, stream: ReadStream<T>, capacity: Int = 256): ReceiveChannel<T> {
    @Suppress("UsePropertyAccessSyntax")
    return toChannel(vertx.getOrCreateContext(), stream, capacity)
}

fun <T> toChannel(context: Context, stream: ReadStream<T>, capacity: Int = 256): ReceiveChannel<T> {
    val ret = ChannelReadStream(context, stream, capacity)

    ret.subscribe()
    return ret
}

@Suppress("unused")
fun <T> toChannel(vertx: Vertx, stream: WriteStream<T>, capacity: Int = 256): SendChannel<T> {
    @Suppress("UsePropertyAccessSyntax")
    return toChannel(vertx.getOrCreateContext(), stream, capacity)
}

fun <T> toChannel(context: Context, stream: WriteStream<T>, capacity: Int = 256): SendChannel<T> {
    val ret = ChannelWriteStream(context, stream, capacity)

    ret.subscribe()
    return ret
}

/**
 * Convert a standard handler to a handler which runs on a coroutine.
 * This is necessary if you want to do fiber blocking synchronous operation in your handler
 */
fun Context.runCoroutine(block: suspend CoroutineScope.() -> Unit) {
    launch(coroutineContext()) {
        try {
            block()
        } catch (e: CancellationException) {
            //skip this exception for coroutine cancel
        }
    }
}

fun Context.coroutineContext(): CoroutineContext {
    require(!isMultiThreadedWorkerContext, { "Must not be a multithreaded worker verticle." })
    return VertxCoroutineDispatcher(this, Thread.currentThread()).asCoroutineDispatcher()
}

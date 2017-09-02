package org.digieng.vertxcoroutines

import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Verticle
import io.vertx.core.Vertx

/**
 * A Verticle which run its start and stop methods in coroutine. You should subclass this class instead of
 * AbstractVerticle to create any verticles that use vertx-kotlin-coroutine. Based on streams work.
 *
 * @author Nick Apperley
 */
@Suppress("unused")
abstract class CoroutineVerticle : Verticle {

    private lateinit var vertxInstance: Vertx
    protected lateinit var context: Context

    override fun init(vertx: Vertx, context: Context) {
        this.vertxInstance = vertx
        this.context = context
    }

    override fun getVertx(): Vertx = vertxInstance

    override fun start(startFuture: Future<Void>?) {
        context.runCoroutine {
            try {
                start()
                startFuture?.complete()
            } catch (t: Throwable) {
                startFuture?.fail(t)
            }
        }
    }

    override fun stop(stopFuture: Future<Void>?) {
        context.runCoroutine {
            try {
                stop()
                stopFuture?.complete()
            } catch (t: Throwable) {
                stopFuture?.fail(t)
            } finally {
                // Do that differently perhaps with a ref count ?
                // removeVertxCoroutineContext()
                TODO("Handle cleanup after an exception has been thrown")
            }
        }
    }

    open suspend fun start() {}

    open suspend fun stop() {}
}
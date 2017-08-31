package org.digieng.vertxcoroutines

import io.vertx.core.Context
import java.util.concurrent.*

internal class VertxCoroutineDispatcher(val vertxContext: Context, val eventLoop: Thread) :
    AbstractExecutorService(),
    ScheduledExecutorService {

    override fun execute(command: Runnable) {
        if (Thread.currentThread() !== eventLoop) vertxContext.runOnContext { command.run() }
        else command.run()
    }

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
        val t = VertxScheduledFuture(vertxContext, command, delay, unit, false)

        t.schedule()
        return t
    }

    override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit?
    ): ScheduledFuture<*> {
        TODO("not implemented")
    }

    override fun <V : Any?> schedule(callable: Callable<V>?, delay: Long, unit: TimeUnit?): ScheduledFuture<V> {
        TODO("not implemented")
    }

    override fun scheduleWithFixedDelay(
        command: Runnable?,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit?
    ): ScheduledFuture<*> {
        TODO("not implemented")
    }

    override fun isTerminated(): Boolean {
        TODO("not implemented")
    }

    override fun shutdown() {
        TODO("not implemented")
    }

    override fun shutdownNow(): MutableList<Runnable> {
        TODO("not implemented")
    }

    override fun isShutdown(): Boolean {
        TODO("not implemented")
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean {
        TODO("not implemented")
    }
}
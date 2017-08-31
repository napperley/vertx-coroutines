package org.digieng.vertxcoroutines

import io.vertx.core.Context
import io.vertx.core.Handler
import java.util.concurrent.Delayed
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class VertxScheduledFuture(
    val vertxContext: Context,
    val task: Runnable,
    val delay: Long,
    val unit: TimeUnit,
    val periodic: Boolean) : ScheduledFuture<Any>, Handler<Long> {

    val done = AtomicInteger(0)
    var id: Long? = null

    fun schedule() {
        val owner = vertxContext.owner()
        id = if (periodic) {
            owner.setTimer(unit.toMillis(delay), this)
        } else {
            owner.setPeriodic(unit.toMillis(delay), this)
        }
    }

    override fun get(): Any? {
        return null
    }

    override fun get(timeout: Long, unit: TimeUnit?): Any? {
        return null
    }

    override fun isCancelled() = done.get() == 1

    override fun handle(event: Long?) {
        if (done.compareAndSet(0, 2)) task.run()
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = if (done.compareAndSet(0, 1)) {
        vertxContext.owner().cancelTimer(id!!)
    } else {
        false
    }

    override fun isDone() = done.get() == 2

    override fun getDelay(unit: TimeUnit?): Long {
        TODO("not implemented")
    }

    override fun compareTo(other: Delayed?): Int {
        TODO("not implemented")
    }
}
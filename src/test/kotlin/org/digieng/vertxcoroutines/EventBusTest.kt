package org.digieng.vertxcoroutines

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contains test cases for EventBus. Based on Julien Viet's work.
 *
 * @author Nick Apperley
 */
@RunWith(VertxUnitRunner::class)
class EventBusTest {

    private lateinit var vertx: Vertx

    @Before
    fun before() {
        @Suppress("INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET")
        vertx = Vertx.vertx()
    }

    @After
    fun after(testCtx: TestContext) {
        vertx.close(testCtx.asyncAssertSuccess())
    }

    @Test
    fun testUnregister(testCtx: TestContext) {
        val bus = vertx.eventBus()
        val consumer = bus.consumer<Int>("the-address")
        val channel = toChannel(vertx, stream = consumer.bodyStream())
        val async = testCtx.async()

        vertx.runCoroutine {
            val list = mutableListOf<Int>()

            println("Processing messages in channel...")
            for (msg in channel) list += msg
            println("List: $list")
            testCtx.assertEquals(listOf(0, 1, 2, 3, 4), list)
        }
        (0..4).forEachIndexed { index, _ ->
            bus.send("the-address", index)
        }
        vertx.setTimer(50L) {
            consumer.unregister()
            println("Unregistered consumer.")
        }
        async.complete()
    }

    private suspend fun replyCoroutine(async: Async?, channel: ReceiveChannel<Int>) {
        val bus = vertx.eventBus()
        var count = 0

        for (msg in channel) {
            val reply = asyncResult<Message<Int?>> { bus.send("another-address", msg, it) }
            val v = reply.body()

            if (v == null) break
            else count += v
        }
        async?.complete()
    }

    private fun replyHandler() = Handler<Message<Int>> { msg ->
        val v = msg.body()

        if (v < 5) {
            println("replying")
            msg.reply(4)
        } else {
            println("ending")
            msg.reply(null)
        }
    }

    @Test
    fun testReply(testCtx: TestContext) {
        val bus = vertx.eventBus()
        val consumer = bus.consumer<Int>("the-address")
        val channel: ReceiveChannel<Int> = toChannel(vertx, consumer.bodyStream())
        val async = testCtx.async()

        bus.consumer<Int>("another-address", replyHandler())
        vertx.runCoroutine { replyCoroutine(async, channel) }
        (0..5).forEachIndexed { index, _ ->
            bus.send("the-address", index)
        }
    }

    @Test
    fun testReplyFailure(testCtx: TestContext) {
        val bus = vertx.eventBus()
        val async = testCtx.async()

        bus.consumer<Int>("the-address") { it.fail(5, "it-failed") }
        vertx.runCoroutine {
            try {
                asyncResult<Message<Int?>> { bus.send("the-address", "the-body", it) }
            } catch (ex: Exception) {
                testCtx.assertTrue(ex is ReplyException)
                val err: ReplyException = ex as ReplyException

                testCtx.assertEquals(5, err.failureCode())
                testCtx.assertEquals(ReplyFailure.RECIPIENT_FAILURE, err.failureType())
                testCtx.assertEquals("it-failed", err.message)
                async.complete()
            }
        }
    }
}
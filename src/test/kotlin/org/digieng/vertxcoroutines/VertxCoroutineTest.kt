package org.digieng.vertxcoroutines

import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contains test cases for VertxCoroutine. Based on stream's work.
 *
 * @author Nick Apperley
 */
@RunWith(VertxUnitRunner::class)
class VertxCoroutineTest {

    @Rule
    @JvmField
    val rule = RunTestOnContext()
    private val ADDRESS1 = "address1"
    private val ADDRESS2 = "address2"
    private val ADDRESS3 = "address3"

    private lateinit var vertx: Vertx
    private lateinit var ai: AsyncInterface

    @Before
    fun before() {
        vertx = rule.vertx()
        ai = AsyncInterfaceImpl(vertx)
    }

    @Test
    fun testContext(testContext: TestContext) {
        val async = testContext.async()
        @Suppress("UsePropertyAccessSyntax")
        val ctx = vertx.getOrCreateContext()

        assertTrue(ctx.isEventLoopContext)
        async.complete()
    }

    @Test
    fun testSleep(testContext: TestContext) {
        val async = testContext.async()

        vertx.runCoroutine {
            val th = Thread.currentThread()
            val cnt = AtomicInteger()
            val periodicTimer = vertx.periodicStream(1L).handler {
                assertSame(Thread.currentThread(), th)
                cnt.incrementAndGet()
            }

            assertSame(Thread.currentThread(), th)
            asyncEvent<Long> { h -> vertx.setTimer(1000L, h) }
            assertTrue(cnt.get() > 900)
            assertSame(Thread.currentThread(), th)
            periodicTimer.cancel()
            async.complete()
        }
    }

    @Test
    fun testFiberHandler(testContext: TestContext) {
        val async = testContext.async()
        val server = vertx.createHttpServer(HttpServerOptions().setPort(8080))

        server.requestHandler({ req ->
            vertx.runCoroutine {
                val res = asyncResult<String> { h -> ai.methodWithParamsAndHandlerNoReturn("oranges", 23, h) }
                assertEquals("oranges23", res)
                req.response().end()
            }
        })
        server.listen { res ->
            assertTrue(res.succeeded())
            val client = vertx.createHttpClient(HttpClientOptions().setDefaultPort(8080))

            client.getNow("/somepath") { resp ->
                assertTrue(resp.statusCode() == 200)
                client.close()
                server.close { _ -> async.complete() }
            }
        }
    }

    @Test
    fun testExecSyncMethodWithParamsAndHandlerNoReturn(testContext: TestContext) {
        val async = testContext.async()
        val th = Thread.currentThread()

        vertx.runCoroutine {
            val res = asyncResult<String> { h -> ai.methodWithParamsAndHandlerNoReturn("oranges", 23, h) }

            assertEquals("oranges23", res)
            assertSame(Thread.currentThread(), th)
            async.complete()
        }
    }

    @Test
    fun testExecSyncMethodWithNoParamsAndHandlerNoReturn(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val res = asyncResult<String> { h -> ai.methodWithNoParamsAndHandlerNoReturn(h) }

        assertEquals("wibble", res)
        async.complete()
    }

    @Test
    fun testExecSyncMethodWithParamsAndHandlerWithReturn(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val res = asyncResult<String> { h -> ai.methodWithParamsAndHandlerWithReturn("oranges", 23, h) }

        assertEquals("oranges23", res)
        async.complete()
    }

    @Test
    fun testExecSyncMethodWithNoParamsAndHandlerWithReturn(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val res = asyncResult<String> { h -> ai.methodWithNoParamsAndHandlerWithReturn(h) }

        assertEquals("wibble", res)
        async.complete()
    }

    @Test
    fun testExecSyncMethodWithNoParamsAndHandlerWithReturnNoTimeout(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val res = withTimeout(2000) {
            asyncResult<String> { h -> ai.methodWithNoParamsAndHandlerWithReturnTimeout(h, 1000) }
        }
        testContext.assertEquals("wibble", res)
        async.complete()
    }

    @Test
    fun testExecSyncMethodWithNoParamsAndHandlerWithReturnTimeout(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()

        try {
            withTimeout(500) {
                asyncResult<String> { h ->
                    ai.methodWithNoParamsAndHandlerWithReturnTimeout(h, 1000)
                }
            }
            testContext.fail()
        } catch (ex: CancellationException) {
            @Suppress("INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET")
            testContext.assertTrue(Context.isOnEventLoopThread())
            async.complete()
        }
    }

    @Test
    fun testExecSyncMethodWithParamsAndHandlerInterface(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val returned = asyncResult<ReturnedInterface> { h ->
            ai.methodWithParamsAndHandlerInterface("apples", 123, h)
        }
        assertNotNull(returned)
        val res = asyncResult<String> { h -> returned.methodWithParamsAndHandlerNoReturn("bananas", 100, h) }

        testContext.assertEquals(res, "bananas100")
        async.complete()
    }

    @Test
    fun testExecSyncMethodThatFails(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()

        try {
            asyncResult<String> { h -> ai.methodThatFails("oranges", h) }
            testContext.fail("Should throw exception")
        } catch (e: Exception) {
            testContext.assertEquals("oranges", e.message)
            async.complete()
        }
    }

    @Test
    fun testReceiveEvent(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val start = System.currentTimeMillis()
        val tid = asyncEvent<Long> { h -> vertx.setTimer(500, h) }
        val end = System.currentTimeMillis()

        assertTrue(end - start >= 500)
        assertTrue(tid >= 0)
        async.complete()
    }

    @Test
    fun testReceiveEventTimeout(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()

        try {
            withTimeout(250) {
                asyncEvent<Long> { h -> vertx.setTimer(500, h) }
            }
            fail()
        } catch (e: CancellationException) {
            @Suppress("INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET")
            testContext.assertTrue(Context.isOnEventLoopThread())
            async.complete()
        }
    }

    @Test
    fun testReceiveEventNoTimeout(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val start = System.currentTimeMillis()
        val tid = withTimeout(1000L) {
            asyncEvent<Long> { h -> vertx.setTimer(500, h) }
        }
        val end = System.currentTimeMillis()

        assertTrue(end - start >= 500)
        assertTrue(tid >= 0L)
        async.complete()
    }

    @Test
    fun testEventMethodFailure(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val cause = RuntimeException()

        try {
            asyncEvent<Any> { _ -> throw cause }
            testContext.fail()
        } catch (ex: Exception) {
            testContext.assertEquals(cause, ex)
        }
        async.complete()
    }

    @Test
    fun testEventMethodFailureNoTimeout(testContext: TestContext) = vertx.runCoroutine {
        val async = testContext.async()
        val cause = RuntimeException()

        try {
            withTimeout(1000L) {
                asyncEvent<Any> { _ -> throw cause }
            }
            testContext.fail()
        } catch (e: Exception) {
            testContext.assertEquals(cause, e)
        }
        async.complete()
    }

    @Test
    fun testHandlerAdaptor(testContext: TestContext) {
        val async = testContext.async()
        val eb = vertx.eventBus()
        // Create a couple of consumers on different addresses. The adaptor allows handler to be used as a Channel.
        val adaptor1 = vertx.receiveChannelHandler<Message<String>>().apply {
            eb.consumer<String>(ADDRESS1).handler(this)
        }
        val adaptor2 = vertx.receiveChannelHandler<Message<String>>().apply {
            eb.consumer<String>(ADDRESS2).handler(this)
        }

        vertx.setPeriodic(10) { _ ->
            eb.send(ADDRESS1, "wibble")
            eb.send(ADDRESS2, "flibble")
        }
        vertx.runCoroutine {
            // Try a receive with timeout.
            var received1: Message<String>? = null
            val adaptor3 = vertx.receiveChannelHandler<Message<String>>()

            for (i in 0..9) {
                val tmpReceived1 = adaptor1.receive()
                assertEquals("wibble", tmpReceived1.body())
                val tmpReceived2 = adaptor2.receive()
                assertEquals("flibble", tmpReceived2.body())
            }

            try {
                received1 = withTimeout(1000) { adaptor1.receive() }
            } catch (ex: CancellationException) {
                testContext.fail("Handler adapter didn't respond within 1s.")
            }

            assertEquals("wibble", received1?.body())
            eb.consumer<String>(ADDRESS3).handler(adaptor3)
            try {
                withTimeout(100) { adaptor3.receive() }
                testContext.fail()
            } catch (ex: CancellationException) {
                async.complete()
            }
        }
    }
}
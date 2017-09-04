@file:Suppress("INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET")

package org.digieng.vertxcoroutines

import io.vertx.core.*

// Contains the test models. Based on stream's work.

typealias AsyncStringHandler = Handler<AsyncResult<String>>
typealias AsyncInterfaceHandler = Handler<AsyncResult<ReturnedInterface>>

interface ReturnedInterface {
    fun methodWithParamsAndHandlerNoReturn(foo: String, bar: Long, resultHandler: AsyncStringHandler)
}

interface AsyncInterface {

    // Non Handler<AsyncResult<>> methods.
    fun someMethod(foo: String, bar: Long): String

    // Methods with Handler<AsyncResult<>>.
    fun methodWithParamsAndHandlerNoReturn(foo: String, bar: Long, resultHandler: AsyncStringHandler)

    fun methodWithNoParamsAndHandlerNoReturn(resultHandler: AsyncStringHandler)

    fun methodWithParamsAndHandlerWithReturn(
        foo: String,
        bar: Long,
        resultHandler: AsyncStringHandler
    ): String

    fun methodWithNoParamsAndHandlerWithReturn(resultHandler: AsyncStringHandler): String

    fun methodWithParamsAndHandlerInterface(
        foo: String,
        bar: Long,
        resultHandler: AsyncInterfaceHandler
    )

    fun methodThatFails(foo: String, resultHandler: AsyncStringHandler)

    fun methodWithNoParamsAndHandlerWithReturnTimeout(resultHandler: AsyncStringHandler, timeout: Long): String
}

class ReturnedInterfaceImpl(private val vertx: Vertx) : ReturnedInterface {
    override fun methodWithParamsAndHandlerNoReturn(
        foo: String,
        bar: Long,
        resultHandler: AsyncStringHandler
    ) {
        vertx.runOnContext { resultHandler.handle(Future.succeededFuture(foo + bar)) }
    }
}

class AsyncInterfaceImpl(private val vertx: Vertx) : AsyncInterface {

    override fun someMethod(foo: String, bar: Long): String {
        return foo + bar
    }

    override fun methodWithParamsAndHandlerNoReturn(
        foo: String,
        bar: Long,
        resultHandler: AsyncStringHandler
    ) {
        vertx.runOnContext { resultHandler.handle(Future.succeededFuture(foo + bar)) }
    }

    override fun methodWithNoParamsAndHandlerNoReturn(resultHandler: AsyncStringHandler) {
        vertx.runOnContext { resultHandler.handle(Future.succeededFuture("wibble")) }
    }

    override fun methodWithParamsAndHandlerWithReturn(
        foo: String,
        bar: Long,
        resultHandler: AsyncStringHandler
    ): String {
        vertx.runOnContext { resultHandler.handle(Future.succeededFuture(foo + bar)) }
        return "ooble"
    }

    override fun methodWithNoParamsAndHandlerWithReturn(resultHandler: AsyncStringHandler): String {
        vertx.runOnContext { resultHandler.handle(Future.succeededFuture("wibble")) }
        return "flooble"
    }

    override fun methodWithParamsAndHandlerInterface(
        foo: String,
        bar: Long,
        resultHandler: AsyncInterfaceHandler
    ) {
        vertx.runOnContext { resultHandler.handle(Future.succeededFuture(ReturnedInterfaceImpl(vertx))) }
    }

    override fun methodThatFails(foo: String, resultHandler: AsyncStringHandler) {
        vertx.runOnContext { resultHandler.handle(Future.failedFuture<String>(VertxException(foo))) }
    }

    override fun methodWithNoParamsAndHandlerWithReturnTimeout(
        resultHandler: AsyncStringHandler,
        timeout: Long
    ): String {
        vertx.setTimer(timeout) {
            resultHandler.handle(Future.succeededFuture("wibble"))
        }
        return "flooble"
    }
}
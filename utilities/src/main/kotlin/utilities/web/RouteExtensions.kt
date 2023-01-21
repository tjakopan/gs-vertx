package utilities.web

import io.vertx.core.Promise
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Route.coroutineHandler(
  coroutineScope: CoroutineScope,
  requestHandler: suspend (RoutingContext) -> Unit
): Route {
  return handler { ctx ->
    coroutineScope.launch(ctx.vertx().dispatcher()) {
      try {
        requestHandler(ctx)
      } catch (e: Exception) {
        ctx.fail(e)
      }
    }
  }
}

fun <T> Route.coroutineRespond(
  coroutineScope: CoroutineScope,
  function: suspend (RoutingContext) -> T?
): Route {
  return respond { ctx ->
    val promise = Promise.promise<T?>()
    coroutineScope.launch(ctx.vertx().dispatcher()) {
      try {
        val t = function(ctx)
        promise.complete(t)
      } catch (e: Exception) {
        promise.fail(e)
      }
    }
    return@respond promise.future()
  }
}

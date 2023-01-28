package utilities.web

import io.vertx.core.Promise
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Route.coroutineHandler(
  coroutineScope: CoroutineScope,
  requestHandler: suspend CoroutineScope.(RoutingContext) -> Unit
): Route {
  return handler { ctx ->
    coroutineScope.launch {
      coroutineScope {
        try {
          requestHandler(ctx)
        } catch (e: Exception) {
          ctx.fail(e)
        }
      }
    }
  }
}

fun <T> Route.coroutineRespond(
  coroutineScope: CoroutineScope,
  function: suspend CoroutineScope.(RoutingContext) -> T
): Route {
  return respond { ctx ->
    val promise = Promise.promise<T>()
    coroutineScope.launch {
      coroutineScope {
        try {
          val t = function(ctx)
          promise.complete(t)
        } catch (e: Exception) {
          promise.fail(e)
        }
      }
    }
    promise.future()
  }
}

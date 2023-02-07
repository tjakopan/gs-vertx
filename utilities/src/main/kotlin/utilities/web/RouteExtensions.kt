package utilities.web

import io.vertx.core.Promise
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Route.handler(
  coroutineScope: CoroutineScope,
  requestHandler: suspend CoroutineScope.(RoutingContext) -> Unit
): Route =
  handler { ctx ->
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

fun <T> Route.respond(coroutineScope: CoroutineScope, function: suspend CoroutineScope.(RoutingContext) -> T): Route =
  respond { ctx ->
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

package utilities.serviceproxy

import io.vertx.core.Future
import io.vertx.core.Promise
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

inline fun CoroutineScope.runService(
  dispatcher: CoroutineDispatcher,
  crossinline function: suspend CoroutineScope.() -> Unit
): Future<Void> {
  val promise = Promise.promise<Void>()
  this.launch(dispatcher) {
    try {
      function()
      promise.complete()
    } catch (e: Throwable) {
      promise.fail(e)
    }
  }
  return promise.future()
}

inline fun <T> CoroutineScope.runServiceWithResult(
  dispatcher: CoroutineDispatcher,
  crossinline function: suspend CoroutineScope.() -> T
): Future<T> {
  val promise = Promise.promise<T>()
  this.launch(dispatcher) {
    try {
      val result = function()
      promise.complete(result)
    } catch (e: Throwable) {
      promise.fail(e)
    }
  }
  return promise.future()
}

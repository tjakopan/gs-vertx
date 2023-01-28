package utilities.serviceproxy

import io.vertx.core.Future
import io.vertx.core.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

inline fun CoroutineScope.runService(crossinline function: suspend CoroutineScope.() -> Unit): Future<Void> {
  val promise = Promise.promise<Void>()
  val job = launch {
    coroutineScope {
      try {
        function()
        promise.complete()
      } catch (e: Throwable) {
        promise.fail(e)
      }
    }
  }
  return promise.future()
    .onComplete { if (job.isActive) job.cancel() }
}

inline fun <T> CoroutineScope.callService(crossinline function: suspend CoroutineScope.() -> T): Future<T> {
  val promise = Promise.promise<T>()
  val job = launch {
    coroutineScope {
      try {
        val result = function()
        promise.complete(result)
      } catch (e: Throwable) {
        promise.fail(e)
      }
    }
  }
  return promise.future()
    .onComplete { if (job.isActive) job.cancel() }
}

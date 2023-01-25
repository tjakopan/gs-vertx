package utilities.sqlclient

import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend inline fun <T> Pool.withTransaction(
  coroutineScope: CoroutineScope,
  crossinline function: suspend CoroutineScope.(SqlConnection) -> T?
): T? {
  return withTransaction { conn ->
    val promise = Promise.promise<T?>()
    coroutineScope.launch {
      coroutineScope {
        try {
          promise.complete(function(conn))
        } catch (e: Throwable) {
          promise.fail(e)
        }
      }
    }
    promise.future()
  }.await()
}

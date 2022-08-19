package redis

import io.vertx.codegen.annotations.ProxyGen
import io.vertx.core.Future

@ProxyGen
interface IReceiver {
  val count: Future<Long>
}
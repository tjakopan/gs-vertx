import io.vertx.ext.web.RoutingContext
import java.util.concurrent.atomic.AtomicLong

private val counter = AtomicLong()

fun getGreetingHandler(ctx: RoutingContext): Greeting {
  val name = ctx.queryParam("name").firstOrNull() ?: "Stranger"
  return Greeting(counter.incrementAndGet(), "Hello, $name!")
}
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxParameterProvider
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension
import kotlin.test.BeforeTest
import kotlin.test.Test

@Suppress("JUnitMalformedDeclaration")
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class, SystemStubsExtension::class)
class HelloWorldApplicationTest {
  private lateinit var client: WebClient

  @Suppress("unused")
  @SystemStub
  private val environmentVariables: EnvironmentVariables =
    EnvironmentVariables(
      VertxParameterProvider.VERTX_PARAMETER_FILENAME,
      {}.javaClass.getResource("vertx-test-config.json")!!.path
    )

  @BeforeTest
  fun setUp(vertx: Vertx) = runTest {
    vertx.deployVerticle(ActuatorServiceVerticle()).await()
    client = WebClient.create(vertx)
  }

  @Test
  fun `should return 200 when sending request to controller`() = runTest {
    val req = client.get(8080, "localhost", "/hello-world")

    val res = req.send().await()

    res.statusCode() shouldBe 200
  }

  @Test
  fun `should return 204 when sending request to health endpoint`() = runTest {
    val req = client.get(8081, "localhost", "/health")

    val res = req.send().await()

    res.statusCode() shouldBe 204
  }

  @Test
  fun `should return 200 when sending request to metrics endpoint`() = runTest {
    val req = client.get(8081, "localhost", "/metrics")

    val res = req.send().await()

    res.statusCode() shouldBe 200
  }
}
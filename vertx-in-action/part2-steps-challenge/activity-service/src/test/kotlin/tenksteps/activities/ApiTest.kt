package tenksteps.activities

import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@Suppress("SqlResolve", "SqlWithoutWhere")
@ExtendWith(VertxExtension::class)
@DisplayName("HTTP API tests")
@Testcontainers
class ApiTest {
  companion object {
    @Suppress("unused")
    @Container
    @JvmStatic
    private val containers = DockerComposeContainer(File("../docker-compose.yml")).apply {
      withExposedService("postgres_1", 5432)
      withExposedService("mongo_1", 27017)
      withExposedService("kafka_1", 19092)
    }
  }

  private val requestSpecification = RequestSpecBuilder()
    .addFilters(listOf(ResponseLoggingFilter(), RequestLoggingFilter()))
    .setBaseUri("http://localhost:3001")
    .build()

  @BeforeTest
  fun setUp(vertx: Vertx): Unit = runTest {
    val insertQuery = "insert into stepevent values($1, $2, $3::timestamp, $4)"
    val now = LocalDateTime.now()
    val data = listOf(
      Tuple.of("123", 1, LocalDateTime.of(2019, 4, 1, 23, 0), 6541),
      Tuple.of("123", 2, LocalDateTime.of(2019, 5, 20, 10, 0), 200),
      Tuple.of("123", 3, LocalDateTime.of(2019, 5, 21, 10, 10), 100),
      Tuple.of("456", 1, LocalDateTime.of(2019, 5, 21, 10, 15), 123),
      Tuple.of("123", 4, LocalDateTime.of(2019, 5, 21, 11, 0), 320),
      Tuple.of("abc", 1, now.minus(1, ChronoUnit.HOURS), 1000),
      Tuple.of("def", 1, now.minus(2, ChronoUnit.HOURS), 100),
      Tuple.of("def", 2, now.minus(30, ChronoUnit.MINUTES), 900),
      Tuple.of("abc", 2, now, 1500)
    )
    val pgPool = PgPool.pool(vertx, pgConnectOptions, poolOptionsOf())
    try {
      pgPool.query("delete from stepevent").execute().await()
      pgPool.preparedQuery(insertQuery).executeBatch(data).await()
    } finally {
      pgPool.close().await()
    }

    vertx.deployVerticle(ActivityApiVerticle()).await()
  }

  @Test
  fun `operate a few successful steps count queries over the dataset`() {
    var jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/456/total")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getInt("count") shouldBe 123

    jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/123/total")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getInt("count") shouldBe 7161

    jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/123/2019/04")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getInt("count") shouldBe 6541

    jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/123/2019/05")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getInt("count") shouldBe 620

    jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/123/2019/05/20")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getInt("count") shouldBe 200
  }

  @Test
  fun `check for http 404 when there is no activity in the dataset`() {
    Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/123/2019/05/18")
    } Then {
      statusCode(404)
    }

    Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/123/2019/03")
    } Then {
      statusCode(404)
    }

    Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/122/total")
    } Then {
      statusCode(404)
    }
  }

  @Test
  fun `check for bad requests (http 400`() {
    Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/123/a/b/c")
    } Then {
      statusCode(400)
    }

    Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/123/a/b")
    } Then {
      statusCode(400)
    }
  }

  @Test
  fun `fetch the ranking over the last 24 hours`() {
    val jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/ranking-last-24-hours")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }
    val data = jsonPath.getList<Map<String, Any>>("$")

    data.size shouldBe 2
    data[0].shouldContain("deviceId" to "abc")
    data[0].shouldContain("stepsCount" to 2500)
    data[1].shouldContain("deviceId" to "def")
    data[1].shouldContain("stepsCount" to 1000)
  }
}
package tenksteps.publicapi

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
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
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tenksteps.activities.ActivityApiVerticle
import tenksteps.userprofiles.UserProfileApiVerticle
import java.io.File
import java.time.LocalDateTime
import kotlin.test.Test

@Suppress("SqlResolve")
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
@TestMethodOrder(OrderAnnotation::class)
@DisplayName("Integration tests for the public API")
@Testcontainers
class IntegrationTest {
  private val requestSpecification = RequestSpecBuilder()
    .addFilters(listOf(ResponseLoggingFilter(), RequestLoggingFilter()))
    .setBaseUri("http://localhost:4000")
    .setBasePath("/api/v1")
    .build()

  companion object {
    @Suppress("unused")
    @Container
    @JvmStatic
    private val containers = DockerComposeContainer(File("../docker-compose.yml")).apply {
      withExposedService("postgres_1", 5432)
      withExposedService("mongo_1", 27017)
      withExposedService("kafka_1", 19092)
    }

    @JvmStatic
    @BeforeAll
    fun setUpAll(vertx: Vertx): Unit = runTest {
      val insertQuery = "insert into stepevent values($1, $2, $3::timestamp, $4)"
      val data = listOf(
        Tuple.of("a1b2c3", 1, LocalDateTime.of(2019, 6, 16, 10, 3), 250),
        Tuple.of("a1b2c3", 2, LocalDateTime.of(2019, 6, 16, 12, 30), 1000),
        Tuple.of("a1b2c3", 3, LocalDateTime.of(2019, 6, 15, 23, 0), 5005)
      )
      val pgPool = PgPool.pool(
        vertx,
        pgConnectOptionsOf(host = "localhost", database = "postgres", user = "postgres", password = "vertx-in-action"),
        poolOptionsOf()
      )
      try {
        pgPool.preparedQuery(insertQuery).executeBatch(data).await()
      } finally {
        pgPool.close().await()
      }

      vertx.deployVerticle(PublicApiVerticle()).await()
      vertx.deployVerticle(UserProfileApiVerticle::class.java.name).await()
      vertx.deployVerticle(ActivityApiVerticle::class.java.name).await()
    }
  }

  private val registrations = mapOf(
    "Foo" to jsonObjectOf(
      "username" to "Foo",
      "password" to "foo-123",
      "email" to "foo@email.me",
      "city" to "Lyon",
      "deviceId" to "a1b2c3",
      "makePublic" to true
    ),
    "Bar" to jsonObjectOf(
      "username" to "Bar",
      "password" to "bar-#$69",
      "email" to "bar@email.me",
      "city" to "Tassin-La-Demi-Lune",
      "deviceId" to "def1234",
      "makePublic" to false
    )
  )

  @Test
  @Order(1)
  fun `register some users`() {
    registrations.values.forEach { registration ->
      Given {
        spec(requestSpecification)
        contentType(ContentType.JSON)
        body(registration.encode())
      } When {
        post("/register")
      } Then {
        statusCode(200)
      }
    }
  }

  private val tokens = mutableMapOf<String, String>()

  @Test
  @Order(2)
  fun `get JWT tokens to access the API`() {
    registrations.forEach { (key, registration) ->
      val login = jsonObjectOf("username" to key, "password" to registration.getString("password"))
      val token = Given {
        spec(requestSpecification)
        contentType(ContentType.JSON)
        body(login.encode())
      } When {
        post("/token")
      } Then {
        statusCode(200)
        contentType("application/jwt")
      } Extract {
        asString()
      }

      token.shouldNotBeNull()
      token.shouldNotBeBlank()

      tokens[key] = token
    }
  }

  @Test
  @Order(3)
  fun `fetch some user`() {
    val jsonPath = Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
    } When {
      get("/Foo")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    val foo = registrations["Foo"]!!
    val props = listOf("username", "email", "city", "deviceId")
    props.forEach { prop -> jsonPath.getString(prop) shouldBe foo.getString(prop) }
    jsonPath.getBoolean("makePublic") shouldBe foo.getBoolean("makePublic")
  }

  @Test
  @Order(4)
  fun `fail at fetching another user data`() {
    Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
    } When {
      get("/Bar")
    } Then {
      statusCode(403)
    }
  }

  @Test
  @Order(5)
  fun `update some user data`() {
    val originalCity = registrations["Foo"]!!.getString("city")
    val originalMakePublic = registrations["Foo"]!!.getBoolean("makePublic")
    val updates = jsonObjectOf("city" to "Nevers", "makePublic" to false)

    Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
      contentType(ContentType.JSON)
      body(updates.encode())
    } When {
      put("/Foo")
    } Then {
      statusCode(200)
    }

    var jsonPath = Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
    } When {
      get("/Foo")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getString("city") shouldBe updates.getString("city")
    jsonPath.getBoolean("makePublic") shouldBe updates.getBoolean("makePublic")

    updates.put("city", originalCity).put("makePublic", originalMakePublic)

    Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
      contentType(ContentType.JSON)
      body(updates.encode())
    } When {
      put("/Foo")
    } Then {
      statusCode(200)
    }

    jsonPath = Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
    } When {
      get("/Foo")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getString("city") shouldBe originalCity
    jsonPath.getBoolean("makePublic") shouldBe originalMakePublic
  }

  @Test
  @Order(6)
  fun `check some user stats`() {
    var jsonPath = Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
    } When {
      get("/Foo/total")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getInt("count") shouldBe 6255

    jsonPath = Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
    } When {
      get("/Foo/2019/06")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getInt("count") shouldBe 6255

    jsonPath = Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
    } When {
      get("/Foo/2019/06/15")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getInt("count") shouldBe 5005
  }

  @Test
  @Order(7)
  fun `check that you cannot access somebody else's stats`() {
    Given {
      spec(requestSpecification)
      headers("Authorization", "Bearer ${tokens["Foo"]}")
    } When {
      get("/Bar/total")
    } Then {
      statusCode(403)
    }
  }
}
package tenksteps.userprofiles

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.restassured.RestAssured.with
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.specification.RequestSpecification
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.IndexOptions
import io.vertx.ext.mongo.MongoClient
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
@DisplayName("User profile API integration tests")
@Testcontainers
class IntegrationTest {
  companion object {
    @Suppress("unused")
    @Container
    @JvmStatic
    private val containers = DockerComposeContainer(File("../docker-compose.yml")).apply {
      withExposedService("mongo_1", 27017)
    }
  }

  private val requestSpecification: RequestSpecification = RequestSpecBuilder()
    .addFilters(listOf(ResponseLoggingFilter(), RequestLoggingFilter()))
    .setBaseUri("http://localhost:3000")
    .build()

  private lateinit var mongoClient: MongoClient

  @BeforeTest
  fun setUp(vertx: Vertx): Unit = runTest {
    val mongoConfig = jsonObjectOf("host" to "localhost", "port" to 27017, "db_name" to "profiles")
    mongoClient = MongoClient.createShared(vertx, mongoConfig)
    mongoClient.createIndexWithOptions("user", jsonObjectOf("username" to 1), IndexOptions().unique(true))
      .await()
    mongoClient.createIndexWithOptions("user", jsonObjectOf("deviceId" to 1), IndexOptions().unique(true))
      .await()
    dropAllUsers()
    vertx.deployVerticle(UserProfileApiVerticle()).await()
  }

  @AfterTest
  fun tearDown(): Unit = runTest {
    try {
      dropAllUsers()
    } finally {
      mongoClient.close().await()
    }
  }

  private suspend fun dropAllUsers() {
    mongoClient.removeDocuments("user", jsonObjectOf()).await()
  }

  private fun basicUser(): JsonObject =
    jsonObjectOf(
      "username" to "abc",
      "password" to "123",
      "email" to "abc@email.me",
      "city" to "Lyon",
      "deviceId" to "a1b2c3",
      "makePublic" to true
    )

  @Test
  fun `register a user`() {
    val response = Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(basicUser().encode())
    } When {
      post("/register")
    } Then {
      statusCode(200)
    } Extract {
      asString()
    }
    response.shouldBeEmpty()
  }

  @Test
  fun `failing to register with an existing username`() {
    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(basicUser().encode())
    } When {
      post("/register")
    } Then {
      statusCode(200)
    }

    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(basicUser().encode())
    } When {
      post("/register")
    } Then {
      statusCode(409)
    }
  }

  @Test
  fun `failing to register a user with an already existing device id`() {
    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(basicUser().encode())
    } When {
      post("/register")
    } Then {
      statusCode(200)
    }

    val user = basicUser().put("username", "Bean")

    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(user.encode())
    } When {
      post("/register")
    } Then {
      statusCode(409)
    }

    runTest {
      val found = mongoClient.findOne("user", jsonObjectOf("username" to "Bean"), jsonObjectOf()).await()
      found.shouldBeNull()
    }
  }

  @Test
  fun `failing to register with missing fields`() {
    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(jsonObjectOf().encode())
    } When {
      post("/register")
    } Then {
      statusCode(400)
    }
  }

  @Test
  fun `failing to register with incorrect field data`() {
    var user = basicUser().put("username", "a b c ")
    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(user.encode())
    } When {
      post("/register")
    } Then {
      statusCode(400)
    }

    user = basicUser().put("deviceId", "@123")
    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(user.encode())
    } When {
      post("/register")
    } Then {
      statusCode(400)
    }

    user = basicUser().put("password", "    ")
    Given {
      spec(requestSpecification)
      contentType(ContentType.JSON)
      accept(ContentType.JSON)
      body(user.encode())
    } When {
      post("/register")
    } Then {
      statusCode(400)
    }
  }

  @Test
  fun `register a user then fetch it`() {
    val user = basicUser()
    with().spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register")

    val jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/${user.getString("username")}")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getString("username") shouldBe user.getString("username")
    jsonPath.getString("email") shouldBe user.getString("email")
    jsonPath.getString("deviceId") shouldBe user.getString("deviceId")
    jsonPath.getString("city") shouldBe user.getString("city")
    jsonPath.getBoolean("makePublic") shouldBe user.getBoolean("makePublic")
    jsonPath.getString("_id").shouldBeNull()
    jsonPath.getString("password").shouldBeNull()
  }

  @Test
  fun `fetching an unknown user`() {
    Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/foo-bar-baz")
    } Then {
      statusCode(404)
    }
  }

  @Test
  fun `register then update a user data`() {
    val original = basicUser()
    with().spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(original.encode())
      .post("/register")

    val updated = basicUser()
      .put("deviceId", "vertx-in-action-123")
      .put("email", "vertx@email-me")
      .put("city", "Nevers")
      .put("makePublic", false)
      .put("username", "Bean")
    with().spec(requestSpecification)
      .contentType(ContentType.JSON)
      .body(updated.encode())
      .put("/${original.getString("username")}")
      .then()
      .statusCode(200)

    val jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/${original.getString("username")}")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getString("username") shouldBe original.getString("username")
    jsonPath.getString("deviceId") shouldBe original.getString("deviceId")
    jsonPath.getString("city") shouldBe updated.getString("city")
    jsonPath.getString("email") shouldBe updated.getString("email")
    jsonPath.getBoolean("makePublic") shouldBe updated.getBoolean("makePublic")
  }

  @Test
  fun `authenticate an existing user`() {
    val user = basicUser()
    val request = jsonObjectOf(
      "username" to user.getString("username"),
      "password" to user.getString("password")
    )

    with().spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register")

    with().spec(requestSpecification)
      .contentType(ContentType.JSON)
      .body(request.encode())
      .post("/authenticate")
      .then()
      .statusCode(200)
  }

  @Test
  fun `failing at authenticating an unknown user`() {
    val request = jsonObjectOf("username" to "Bean", "password" to "abc")

    with().spec(requestSpecification)
      .contentType(ContentType.JSON)
      .body(request.encode())
      .post("/authenticate")
      .then()
      .statusCode(401)
  }

  @Test
  fun `find who owns a device`() {
    val user = basicUser()
    with().spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register")

    val jsonPath = Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/owns/${user.getString("deviceId")}")
    } Then {
      statusCode(200)
    } Extract {
      jsonPath()
    }

    jsonPath.getString("username") shouldBe user.getString("username")
    jsonPath.getString("deviceId") shouldBe user.getString("deviceId")
    jsonPath.getString("cits").shouldBeNull()

    Given {
      spec(requestSpecification)
      accept(ContentType.JSON)
    } When {
      get("/owns/404")
    } Then {
      statusCode(404)
    }
  }
}
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.ext.web.multipart.MultipartForm
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.ext.web.client.webClientOptionsOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import storage.StorageConfig
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.test.BeforeTest
import kotlin.test.Test

@Suppress("JUnitMalformedDeclaration")
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(VertxExtension::class)
internal class FileUploadIT {
  private lateinit var uploadDir: Path
  private lateinit var client: WebClient

  @BeforeTest
  fun setUp(vertx: Vertx, @TempDir tempDir: Path) = runTest {
    uploadDir = tempDir
    val config = json { obj("storage" to StorageConfig(tempDir.absolutePathString()).toJson()) }
    vertx.deployVerticle(UploadingFilesVerticle(), deploymentOptionsOf(config = config)).await()
    client = WebClient.create(
      vertx,
      webClientOptionsOf(defaultHost = "localhost", defaultPort = 8080, followRedirects = false)
    )
  }

  @Test
  fun `should upload file`(vertx: Vertx, @TempDir tempDir: Path) = runTest {
    val file = "Hello, World".toTempFile(vertx, tempDir)
    val form = MultipartForm.create()
      .binaryFileUpload("file", file.name, file.absolutePath, "text/plain")

    val response = client.post("/")
      .sendMultipartForm(form)
      .await()

    response.statusCode() shouldBe 302
    val uploadedFile = uploadDir.resolve(file.name)
    uploadedFile.exists() shouldBe true
  }

  @Test
  fun `should download file`(vertx: Vertx) = runTest {
    val file = "Hello, World".toTempFile(vertx, uploadDir)

    val response = client.get("/files/${file.name}")
      .`as`(BodyCodec.string())
      .send()
      .await()

    response.statusCode() shouldBe 200
    response.headers()["Content-Disposition"] shouldBe "attachment; filename=\"${file.name}\""
    response.body() shouldBe "Hello, World"
  }

  @Test
  fun `should 404 when missing file`(vertx: Vertx) = runTest {
    val response = client.get("/files/foo.txt")
      .send()
      .await()

    response.statusCode() shouldBe 404
  }

  @Test
  fun `should list all files`(vertx: Vertx) = runTest {
    val file1 = "Hello, World".toTempFile(vertx, uploadDir)
    val file2 = "Hello, World 2".toTempFile(vertx, uploadDir)

    val response = client.get("/")
      .`as`(BodyCodec.string())
      .send()
      .await()

    response.body() shouldContain file1.name
    response.body() shouldContain file2.name
  }

  private suspend fun String.toTempFile(vertx: Vertx, tempDir: Path): File {
    val path = vertx.fileSystem().createTempFile(tempDir.absolutePathString(), null, null, null as String?).await()
    val buffer = Buffer.buffer(this)
    vertx.fileSystem().writeFile(path, buffer).await()
    return File(path)
  }
}
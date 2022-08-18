package storage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.coroutines.await
import io.vertx.serviceproxy.ServiceException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("JUnitMalformedDeclaration")
@ExtendWith(VertxExtension::class)
internal class FileSystemStorageServiceTest {
  private lateinit var testScope: TestScope
  private lateinit var config: StorageConfig
  private lateinit var service: FileSystemStorageService

  @BeforeTest
  fun setUp(vertx: Vertx, @TempDir tempDir: Path) {
    testScope = TestScope()
    config = StorageConfig(tempDir.absolutePathString())
    service = FileSystemStorageService(testScope, vertx, config)
  }

  @Test
  fun `load non existent`() = testScope.runTest {
    val e = shouldThrow<ServiceException> { service.load("foo.txt").await() }
    e.failureCode() shouldBe FILE_NOT_FOUND_ERROR
  }

  @Test
  fun `save and load`(vertx: Vertx, @TempDir tempDir: Path) = testScope.runTest {
    service.store("Hello, World".toUploadedFileDto(vertx, tempDir, "foo.txt")).await()
    val path = service.load("foo.txt").await()

    Path(path).exists() shouldBe true
  }

  @Test
  fun `save relative path not permitted`(vertx: Vertx, @TempDir tempDir: Path) = testScope.runTest {
    shouldThrowWithMessage<ServiceException>("Cannot store file outside of current directory.") {
      service.store("Hello, World".toUploadedFileDto(vertx, tempDir, "../foo.txt")).await()
    }
  }

  @Test
  fun `save absolute path not permitted`(vertx: Vertx, @TempDir tempDir: Path) = testScope.runTest {
    shouldThrowWithMessage<ServiceException>("Cannot store file outside of current directory.") {
      service.store("Hello, World".toUploadedFileDto(vertx, tempDir, "/etc/passwd")).await()
    }
  }

  @Test
  fun `save permitted`(vertx: Vertx, @TempDir tempDir: Path) = testScope.runTest {
    service.store("Hello, World".toUploadedFileDto(vertx, tempDir, "bar/../foo.txt")).await()
  }

  private suspend fun String.toUploadedFileDto(vertx: Vertx, tempDir: Path, fileName: String): UploadedFileDto {
    val path = vertx.fileSystem().createTempFile(tempDir.absolutePathString(), null, null, null as String?).await()
    val buffer = Buffer.buffer(this)
    vertx.fileSystem().writeFile(path, buffer).await()
    return UploadedFileDto(fileName, path, buffer.length().toLong())
  }
}
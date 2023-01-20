package storage

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.file.FileSystem
import io.vertx.kotlin.core.file.copyOptionsOf
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.serviceproxy.ServiceException
import kotlinx.coroutines.CoroutineScope
import utilities.serviceproxy.runService
import utilities.serviceproxy.callService
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

const val STORAGE_ERROR: Int = 1
const val FILE_NOT_FOUND_ERROR: Int = 2

class FileSystemStorageService(private val scope: CoroutineScope, private val vertx: Vertx, config: StorageConfig) :
  IStorageService {
  private val fileSystem: FileSystem = vertx.fileSystem()
  private val rootPath: Path = Path(config.location)

  override fun init(): Future<Void> =
    scope.runService(vertx.dispatcher()) { fileSystem.mkdirs(rootPath.pathString).await() }

  override fun store(file: UploadedFileDto): Future<Void> = scope.runService(vertx.dispatcher()) {
    if (file.isEmpty()) throw ServiceException(STORAGE_ERROR, "Failed to store empty file.")
    val destPath = rootPath.resolve(file.originalFileName).normalize().toAbsolutePath()
    // This is a security check
    if (destPath.parent != rootPath.toAbsolutePath())
      throw ServiceException(STORAGE_ERROR, "Cannot store file outside of current directory.")
    fileSystem.copy(file.uploadedFilePath, destPath.absolutePathString(), copyOptionsOf(replaceExisting = true)).await()
  }

  override fun loadAll(): Future<List<String>> =
    scope.callService(vertx.dispatcher()) { fileSystem.readDir(rootPath.absolutePathString()).await() }

  override fun load(filename: String): Future<String> = scope.callService(vertx.dispatcher()) {
    val path = rootPath.resolve(filename).absolutePathString()
    val exists = fileSystem.exists(path).await()
    if (exists) path
    else throw ServiceException(FILE_NOT_FOUND_ERROR, "Could not read $filename.")
  }

  override fun deleteAll(): Future<Void> = scope.runService(vertx.dispatcher()) {
    val path = rootPath.absolutePathString()
    val exists = fileSystem.exists(path).await()
    if (exists) fileSystem.deleteRecursive(path, true).await()
  }
}
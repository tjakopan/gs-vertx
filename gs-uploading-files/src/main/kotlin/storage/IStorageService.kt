package storage

import io.vertx.codegen.annotations.ProxyGen
import io.vertx.core.Future
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope

@ProxyGen
interface IStorageService {
  fun init(): Future<Void>
  fun store(file: UploadedFileDto): Future<Void>
  fun loadAll(): Future<List<String>>
  fun load(filename: String): Future<String>
  fun deleteAll(): Future<Void>
}

fun createStorageService(scope: CoroutineScope, vertx: Vertx, config: StorageConfig): IStorageService =
  FileSystemStorageService(scope, vertx, config)

fun createStorageServiceProxy(vertx: Vertx, address: String): IStorageService =
  IStorageServiceVertxEBProxy(vertx, address)
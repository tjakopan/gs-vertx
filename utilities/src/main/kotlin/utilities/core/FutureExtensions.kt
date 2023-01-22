package utilities.core

import io.vertx.core.Future

fun Future<Void>.replaceWithUnit(): Future<Unit> = map { }
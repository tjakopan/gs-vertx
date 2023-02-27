package tenksteps.publicapi

import io.vertx.core.buffer.Buffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

internal fun publicKey(): Buffer = read("public_key.pem")

internal fun privateKey(): Buffer = read("private_key.pem")

private fun read(file: String): Buffer {
  var path = Paths.get("vertx-in-action/part2-steps-challenge/public-api", file)
  if (!path.toFile().exists()) path = Paths.get("..", "public-api", file)
  return Buffer.buffer(Files.readAllLines(path, StandardCharsets.UTF_8).joinToString(separator = "\n"))
}
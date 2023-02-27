package tenksteps.congrats

import io.vertx.kafka.client.serialization.JsonObjectDeserializer
import org.apache.kafka.common.serialization.StringDeserializer

internal fun kafkaConsumerConfig(group: String): Map<String, String> =
  mapOf(
    "bootstrap.servers" to "localhost:19092",
    "key.deserializer" to StringDeserializer::class.java.name,
    "value.deserializer" to JsonObjectDeserializer::class.java.name,
    "auto.offset.reset" to "earliest",
    "enable.auto.commit" to "true",
    "group.id" to group
  )
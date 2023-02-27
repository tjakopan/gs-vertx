package tenksteps.eventstats

import io.vertx.kafka.client.serialization.JsonObjectDeserializer
import io.vertx.kafka.client.serialization.JsonObjectSerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer

internal val kafkaProducerConfig: Map<String, String> =
  mapOf(
    "bootstrap.servers" to "localhost:19092",
    "key.serializer" to StringSerializer::class.java.name,
    "value.serializer" to JsonObjectSerializer::class.java.name,
    "acks" to "1"
  )

internal fun kafkaConsumerConfig(group: String): Map<String, String> =
  mapOf(
    "bootstrap.servers" to "localhost:19092",
    "key.deserializer" to StringDeserializer::class.java.name,
    "value.deserializer" to JsonObjectDeserializer::class.java.name,
    "auto.offset.reset" to "earliest",
    "enable.auto.commit" to "true",
    "group.id" to group
  )
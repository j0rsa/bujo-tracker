package com.j0rsa.bujo.tracker.projections

import com.j0rsa.bujo.tracker.TagActionCreated
import com.j0rsa.bujo.tracker.Event
import com.j0rsa.bujo.tracker.HabitCreated
import com.j0rsa.bujo.tracker.service.CacheService
import io.vertx.core.Vertx
import io.vertx.kafka.client.consumer.KafkaConsumer

class TagsProjection(val vertx: Vertx) {
    private val config = mapOf(
        "bootstrap.servers" to "localhost:9092",
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "my_group",
    )
    private val consumer = KafkaConsumer.create<String, Event>(vertx, config)
    fun start() {
        //TODO: replace with event bus (and kafka under kafka bridge)
        consumer.handler {record ->
            with(record.value()) {
                when(this) {
                    is HabitCreated -> CacheService.sadd(listOf(this)) {
                        it.userId.value.toString() to it.tag.toSet()
                    }
                    is TagActionCreated -> CacheService.sadd(listOf(this)) {
                        it.userId.value.toString() to setOf(it.tag)
                    }
                    else -> {}
                }
            }
        }
        consumer.subscribe("events") {
            if (it.succeeded()) {
                println("Subscribed")
            }
        }

    }
}
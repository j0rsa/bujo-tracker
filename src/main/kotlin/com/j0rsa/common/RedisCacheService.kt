package com.j0rsa.common

import io.lettuce.core.LettuceFutures
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
open class RedisCacheService(
    redisHost: String,
    port: Int = 6379,
    private val oneRecordMaxAwait: Duration = 1.seconds
) {
    private val client = RedisClient.create("redis://$redisHost:$port/")
    private val redisConnect = client.connect()
    private val redisCommands = redisConnect.async().apply {
        setAutoFlushCommands(false)
    }

    fun <T> set(list: List<T>, expiration: Duration? = null, keyValueTransformer: (T) -> Pair<String, String>) {
        val futures: MutableList<RedisFuture<*>> = mutableListOf()
        list.forEach { item ->
            with(keyValueTransformer(item)) {
                redisCommands.set(first, second)
                expiration?.let {
                    redisCommands.expire(first, it.inSeconds.toLong())
                }
            }
        }
        redisCommands.flushCommands()
        LettuceFutures.awaitAll(list.size * oneRecordMaxAwait.inSeconds.toLong(), TimeUnit.SECONDS, *futures.toTypedArray())
    }

    fun <T> sadd(list: List<T>, expiration: Duration? = null, keyValueTransformer: (T) -> Pair<String, Set<String>>) {
        val futures: MutableList<RedisFuture<*>> = mutableListOf()
        list.forEach { item ->
            with(keyValueTransformer(item)) {
                redisCommands.sadd(first, *second.toTypedArray())
                expiration?.let {
                    redisCommands.expire(first, it.inSeconds.toLong())
                }
            }
        }
        redisCommands.flushCommands()
        LettuceFutures.awaitAll(list.size * oneRecordMaxAwait.inSeconds.toLong(), TimeUnit.SECONDS, *futures.toTypedArray())
    }

    fun <T> get(key: String, transformer: (String) -> T): T? =
        try {
            val future = redisCommands.get(key)
            future.await(oneRecordMaxAwait.inSeconds.toLong(), TimeUnit.SECONDS)
            transformer(future.get())
        } catch (e: InterruptedException) {
            null
        }
}
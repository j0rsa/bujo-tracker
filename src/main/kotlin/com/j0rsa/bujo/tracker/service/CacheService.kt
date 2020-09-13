package com.j0rsa.bujo.tracker.service

import com.j0rsa.bujo.tracker.Config
import com.j0rsa.common.RedisCacheService
import kotlin.time.ExperimentalTime

@ExperimentalTime
object CacheService : RedisCacheService(Config.app.redis.host)
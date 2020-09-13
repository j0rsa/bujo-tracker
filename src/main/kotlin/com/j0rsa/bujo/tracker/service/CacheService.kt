package com.j0rsa.bujo.tracker.service

import com.j0rsa.bujo.tracker.Config
import com.j0rsa.common.RedisCacheService

object CacheService : RedisCacheService(Config.app.redis.host)
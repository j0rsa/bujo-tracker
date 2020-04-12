package com.j0rsa.bujo.tracker.handler

import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.handler.CorsHandler

class Cors {
    companion object {
        private val allowedHeaders = setOf(
            "x-requested-with",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Headers",
            "Access-Control-Allow-Credentials",
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "X-Auth-Id"
        )
        private val allowedMethods = setOf(
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.OPTIONS,
            HttpMethod.DELETE,
            HttpMethod.PATCH,
            HttpMethod.PUT
        )

        fun enable(): CorsHandler =
            CorsHandler.create("*")
                .allowedHeaders(allowedHeaders)
                .allowedMethods(allowedMethods)
    }
}
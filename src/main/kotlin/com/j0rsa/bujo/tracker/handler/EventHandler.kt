package com.j0rsa.bujo.tracker.handler

import com.j0rsa.bujo.tracker.Event
import com.j0rsa.bujo.tracker.Logging
import com.j0rsa.bujo.tracker.consume
import com.j0rsa.bujo.tracker.logger
import io.vertx.kotlin.coroutines.CoroutineVerticle

const val ACTIONS = "actions:get"

class EventHandler : CoroutineVerticle(), Logging {
	private val logger = logger()

	override suspend fun start() {
		consume(ACTIONS, ::process)
	}

	private fun process(event: Event) {
		logger.debug("Processing $event")
	}
}
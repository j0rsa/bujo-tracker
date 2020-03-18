package com.j0rsa.bujo.tracker.handler

enum class ResponseState(val value: Int) {
	OK(200),
	CREATED(201),
	NO_CONTENT(204),
	NOT_FOUND(404),
	INTERNAL_SERVER_ERROR(500)
}

data class Response<T>(val state: ResponseState, val value: T? = null)
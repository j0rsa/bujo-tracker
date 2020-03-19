package com.j0rsa.bujo.tracker

sealed class TrackerError {
	object NotFound : TrackerError()
	data class SyStemError(val message: String) : TrackerError()
}

typealias NotFound = TrackerError.NotFound
typealias SystemError = TrackerError.SyStemError
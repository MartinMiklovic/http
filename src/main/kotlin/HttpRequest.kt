package main

data class HttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
)
package main

import java.io.ByteArrayOutputStream

const val CONTENT_LENGTH = "Content-Length"

class HttpResponse(
    private val statusCode: Int,
    private val headers: Map<String, String> = emptyMap(),
    private val responseBody: ByteArray? = null,
) {

    fun toByteArray(): ByteArray = with(ByteArrayOutputStream()) {
        write("$PROTOCOL_VERSION $statusCode".toByteArray())
        write(CRLF.toByteArray())

        headers.forEach { (key, value) ->
            write("$key: $value".toByteArray())
            write(CRLF.toByteArray())
        }

        if (responseBody != null) {
            write("${CONTENT_LENGTH}: ${responseBody.size}".toByteArray())
            write(CRLF.toByteArray())
        }
        write(CRLF.toByteArray())
        if (responseBody != null) {
            write(responseBody)
        }

        toByteArray()
    }

}
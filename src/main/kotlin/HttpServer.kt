package main

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

private const val N_THREADS = 10
private const val HTTP_GET = "GET"


data class HttpServer(private val port: Int) {

    private val getEndpoints: MutableList<Endpoint> = mutableListOf()


    data class Endpoint(
        val path: String,
        val resolver: (httpRequest: HttpRequest) -> HttpResponse,
    )


    fun get(path: String, resolver: (httpRequest: HttpRequest) -> HttpResponse) {
        getEndpoints.add(Endpoint(path, resolver))
    }

    fun post(path: String, resolver: (httpRequest: HttpRequest) -> HttpResponse) {
        getEndpoints.add(Endpoint(path, resolver))
    }
    fun put(path: String, resolver: (httpRequest: HttpRequest) -> HttpResponse) {
        getEndpoints.add(Endpoint(path, resolver))
    }
    fun delete(path: String, resolver: (httpRequest: HttpRequest) -> HttpResponse) {
        getEndpoints.add(Endpoint(path, resolver))
    }

    fun start() {
        val threadPool = Executors.newFixedThreadPool(N_THREADS)

        ServerSocket(port).use { server ->
            println("Listening on port $port")

            while (true) {
                val socket = server.accept()
                println("Accepted connection from ${socket.remoteSocketAddress}")

                threadPool.execute {
                    handleConnection(socket)
                }
            }
        }
    }

    fun handleConnection(socket: Socket) {
        socket.use {
            val input = it.getInputStream()
            val output = it.getOutputStream()
            val (rawHeaders, body) = readHeaders(input)
            val headers = parseHeaders(rawHeaders)
            val response = resolveResponse(headers)

            output.write(response.toByteArray())
            output.flush()
        }
    }

    fun readHeaders(input: InputStream): Pair<List<String>, ByteArray> {
        val buffer = ByteArray(8)
        val output = ByteArrayOutputStream()
        var bodyStart = -1
        var state = 0
        val headers = mutableListOf<String>()

        while (true) {
            val n = input.read(buffer)
            if (n == -1) {
                error("Connection closed unexpectedly")
            }

            for (i in 0 until n) {
                val b = buffer[i]

                output.write(b.toInt())

                when (state) {

                    0 -> if (b == CARRIAGE_RETURN) state = 1

                    1 -> if (b == NEW_LINE) {
                        state = 2
                        val outputByteArray = output.toByteArray()
                        headers.add(
                            outputByteArray.copyOfRange(
                                0, outputByteArray.size - 2
                            ).decodeToString()
                        )
                        output.reset()
                    } else state = 0


                    2 -> if (b == CARRIAGE_RETURN) state = 3 else state = 0

                    3 -> if (b == NEW_LINE) {
                        bodyStart = i + 1
                        break
                    } else state = 0
                }
            }
            if (bodyStart != -1) {
                val remaningBody = buffer.copyOfRange(bodyStart, n)
                return headers to remaningBody
            }
        }
    }

    fun parseHeaders(headers: List<String>): HttpRequest {
        val startLine = headers[0].split(" ")
        val headersMap = mutableMapOf<String, String>()

        for (i in 1 until headers.size) {
            val header = headers[i].split(":").map { it.trim() }
            headersMap.put(header[0], header[1])
        }

        return HttpRequest(
            method = startLine[0],
            path = startLine[1],
            headers = headersMap,
        )
    }

    fun resolveResponse(httpRequest: HttpRequest): HttpResponse {
        val endpoint = when (httpRequest.method) {
            HTTP_GET ->
                getEndpoints.firstOrNull {
                    it.path == httpRequest.path
                }

            else -> null
        }

        if (endpoint == null) {
            println("Endpoint with path ${httpRequest.path} not found")
            return HttpResponse(404)
        }

        return endpoint.resolver(httpRequest)
    }

}

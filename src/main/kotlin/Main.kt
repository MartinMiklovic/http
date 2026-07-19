package main

import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket

const val PORT = 4221

fun main() {
    ServerSocket(PORT).use { server ->
        println("Listening on port $PORT")

        while (true) {
            val socket = server.accept()
            println("Accepted connection from ${socket.remoteSocketAddress}")

            Thread {
                handleConnection(socket)
            }.start()
        }
    }
}

fun handleConnection(socket: Socket) {
    socket.use {
        val input = it.getInputStream()
        val output = it.getOutputStream()

        for (line in getLinesSequence(input)) {
            println(line)

            if (line.isEmpty()) {
                break
            }
        }

        output.write("HTTP/1.1 200 OK\r\n\r\n".toByteArray())
        output.flush()
    }
}

fun getLinesSequence(input: InputStream) = sequence {
    val buffer = ByteArray(8)
    val currentLine = StringBuilder()

    while (true) {
        val n = input.read(buffer)
        if (n == -1) {
            if (currentLine.isNotEmpty()) {
                yield(currentLine.toString())
            }
            break
        }

        for (i in 0 until n) {
            val c = buffer[i].toInt().toChar()

            if (c == '\r') {
                continue
            }

            if (c == '\n') {
                yield(currentLine.toString())
                currentLine.clear()
            } else {
                currentLine.append(c)
            }
        }
    }
}
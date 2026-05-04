package com.ganaderia

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8080, // AWS inyecta el PORT
        host = "0.0.0.0"
    ) {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respondText("OK") // CloudWatch lo usará para health check
        }
        // aquí irán tus rutas de animales, vacunas, etc.
    }
}

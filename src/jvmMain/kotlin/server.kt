package com.villevalois.fuji

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json(
                Json(JsonConfiguration.Stable)
            )
        }
        install(CORS) {
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Delete)
            anyHost()
        }
        install(Compression) {
            gzip()
        }
        install(WebSockets) {
            pingPeriod = Duration.ofMinutes(1)
        }

        routing {
            route("/api") {
                get("/cameras") { loggedResponse(crawlFujiCameras()) }
                get("/lenses") { loggedResponse(crawlFujiLenses()) }
                get("/camaraCamerasDeals") { loggedResponse(crawlCamaraCameras()) }
                get("/camaraLensesDeals") { loggedResponse(crawlCamaraLenses()) }
                get("/camaraCamerasAds") { loggedResponse(crawlCamaraCamerasAds()) }
                get("/camaraLensesAds") { loggedResponse(crawlCamaraLensesAds()) }
                get("/imagesPhotoCamerasAds") { loggedResponse(crawlImagesPhotoCameraAds()) }
                get("/imagesPhotoLensesAds") { loggedResponse(crawlImagesPhotoLensesAds()) }
            }
            webSocket("/ws") {

            }
            static {
                defaultResource("index.html", "web")
                resource("output.js")
                resources("web")
            }
        }
    }.start(wait = true)
}

private suspend fun PipelineContext<Unit, ApplicationCall>.loggedResponse(response: Any) {
    try {
        call.respond(response)
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}

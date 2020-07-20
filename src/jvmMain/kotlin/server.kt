package com.villevalois.fuji

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import kotlinx.html.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

fun HTML.index() {
    attributes["style"] = "overflow-x: hidden;"
    head {
        title("Hello from Ktor!")
    }
    body {
        div {
            id = "root"
        }
        script(src = "/static/output.js") {}
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
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

        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
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
            static("/static") {
                resources()
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

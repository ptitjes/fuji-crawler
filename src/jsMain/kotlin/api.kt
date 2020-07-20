package com.villevalois.fuji.frontend

import com.villevalois.fuji.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val endpoint = window.location.origin

val jsonClient = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer(
            Json(JsonConfiguration.Stable)
        )
    }
}

suspend fun getCameras(): List<Camera> {
    return jsonClient.get("$endpoint/api/cameras")
}

suspend fun getLenses(): List<Lens> {
    return jsonClient.get("$endpoint/api/lenses")
}

suspend fun getCamaraCamerasDeals(): Map<String, List<ProductNewDeal>> {
    return jsonClient.get("$endpoint/api/camaraCamerasDeals")
}

suspend fun getCamaraLensesDeals(): Map<String, List<ProductNewDeal>> {
    return jsonClient.get("$endpoint/api/camaraLensesDeals")
}

suspend fun getCamaraCamerasAds(): Map<String, List<ProductSecondHandDeal>> {
    return jsonClient.get("$endpoint/api/camaraCamerasAds")
}

suspend fun getCamaraLensesAds(): Map<String, List<ProductSecondHandDeal>> {
    return jsonClient.get("$endpoint/api/camaraLensesAds")
}

suspend fun getImagesPhotoCamerasAds(): Map<String, List<ProductSecondHandDeal>> {
    return jsonClient.get("$endpoint/api/imagesPhotoCamerasAds")
}

suspend fun getImagesPhotoLensesAds(): Map<String, List<ProductSecondHandDeal>> {
    return jsonClient.get("$endpoint/api/imagesPhotoLensesAds")
}

fun getCameraAds(): Flow<Map<String, List<ProductSecondHandDeal>>> = flow {
    var ads = getCamaraCamerasAds()
    emit(ads)
    ads = ads.merge(getImagesPhotoCamerasAds())
    emit(ads)
}

fun getLensesAds(): Flow<Map<String, List<ProductSecondHandDeal>>> = flow {
    var ads = getCamaraLensesAds()
    emit(ads)
    ads = ads.merge(getImagesPhotoLensesAds())
    emit(ads)
}

fun <K, V> Map<K, List<V>>.merge(other: Map<K, List<V>>): Map<K, List<V>> =
    (this.keys + other.keys).associateWith { (this[it] ?: listOf()) + (other[it] ?: listOf()) }

package com.villevalois.fuji

import com.villevalois.fuji.utils.StringSanitizer
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.parse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

const val imagesPhotoWebsite = "https://www.images-photo.com"

suspend fun crawlImagesPhotoCameraAds(): Map<String, List<ProductSecondHandDeal>> {
    return crawlImagesPhotoAjaxQuery(mapOf(
        "id_category_layered" to 6,
        "layered_manufacturer_3" to 3,
    )) { name -> sanitizeCameraName(name) }
}

suspend fun crawlImagesPhotoLensesAds(): Map<String, List<ProductSecondHandDeal>> {
    val fujiLensesCharacteristics = crawlFujiLenses().mapNotNull { it.characteristics }

    return crawlImagesPhotoAjaxQuery(mapOf(
        "id_category_layered" to 5,
        "layered_manufacturer_3" to 3,
    )) { name ->
        val sanitizedName = sanitizeAdsLensName(name.toUpperCase())
        val characteristics = lensCharacteristicsFromName(sanitizedName)
            ?.let { inferClosestCharacteristics(it, fujiLensesCharacteristics) }
        characteristics?.normalizedId()
    }
}

suspend fun crawlImagesPhotoAjaxQuery(
    parameters: Map<String, Any?>,
    nameSanitizer: (String) -> String?,
): Map<String, List<ProductSecondHandDeal>> {

    val client = HttpClient() {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    val payload: AjaxResultsPayload = client.use {
        val text = it.get<String>("$imagesPhotoWebsite/modules/advancedfeaturesvalues/blocklayered-ajax.php") {
            accept(ContentType.Application.Json)
            parameters.forEach { (key, value) -> parameter(key, value) }
        }
        Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true)).parse(text)
    }
    return crawlProductList(Jsoup.parse(payload.productList), nameSanitizer)
}

fun crawlImagesPhotoAdsPage(
    page: String,
    nameSanitizer: (String) -> String?,
): Map<String, List<ProductSecondHandDeal>> {
    val content = Jsoup.connect(page).userAgent(USER_AGENT).get();
    return crawlProductList(content, nameSanitizer)
}

private fun crawlProductList(
    content: Document,
    nameSanitizer: (String) -> String?,
): Map<String, List<ProductSecondHandDeal>> {
    val ads = content.select(".product_list .produit").mapNotNull { ad ->

        val link = ad.select(".nom a")
        val name = nameSanitizer(link.text())

        if (name == null) println(link.text())

        if (name == null || ad.text().contains("Vendu")) null
        else {
            val url = link.attr("href")
            val title = sanitizeAdsTitle(link.text())

            val price = parseImagesPhotoAdsPrice(ad.select(".price").first().text())

            val imageUrl = ad.select(".img-content img").first().attr("src")
            name to ProductSecondHandDeal("Images Photo", price, url, title, imageUrl)
        }
    }
    return ads
        .groupBy({ it.first }, { it.second })
}

private fun parseImagesPhotoAdsPrice(priceText: String) =
    priceText.trim().removeSuffix(" â\u0082¬").replace(" ", "").replace(',', '.').toDouble()

private fun sanitizeAdsTitle(titleText: String) =
    adsTitleSanitizer.sanitize(titleText)

private val adsTitleSanitizer = StringSanitizer(
    // Remove Fuji brand names
    "(fujifilm|Fujifilm|FUJIFILM)" to { "" },
    "(fuji|Fuji|FUJI)" to { "" },
)

private fun sanitizeCameraName(titleText: String) =
    cameraNameSanitizer.sanitize(titleText.toLowerCase())

private val cameraNameSanitizer = StringSanitizer(
    // Remove Fuji brand names
    "fujifilm" to { "" },
    "fuji" to { "" },
    // Clean camera names
    "x[- ]*([a-zA-Z]+)[- ]*([0-9]+)" to { "x-${it.groupValues[1]}${it.groupValues[2]}" },
    "x(f|p)?[- ]*([0-9]+)[- ]*(v|f)?" to { "x${it.groupValues[1]}${it.groupValues[2]}${it.groupValues[3]}" },
    "gfx[- ]*([0-9]+)[- ]*(s|r)?" to { "gfx ${it.groupValues[1]}${it.groupValues[2]}" },
    // Keep only the camera names
    "(x[^ +,]*|gfx [^ +,]*).*" to { it.groupValues[1] },
)

@Serializable
data class AjaxResultsPayload(
    val productList: String,
)

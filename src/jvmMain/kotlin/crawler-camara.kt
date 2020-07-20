package com.villevalois.fuji

import com.villevalois.fuji.utils.StringSanitizer
import org.jsoup.Jsoup
import kotlin.math.round

const val camaraWebsite = "https://www.camara.net"

fun crawlCamaraCameras(): Map<String, List<ProductNewDeal>> {
    val baseCamerasPath = "$camaraWebsite/appareils-photo"
    val cameras = crawlCamaraWebsiteForm("$baseCamerasPath/hybrides") +
            crawlCamaraWebsiteForm("$baseCamerasPath/moyen-format")
    return cameras.groupBy({ sanitizeCamaraCameraName(it.first) }, { it.second })
}

fun crawlCamaraLenses(): Map<String, List<ProductNewDeal>> {
    val fujiLensesCharacteristics = crawlFujiLenses().mapNotNull { it.characteristics }

    val baseLensesPath = "$camaraWebsite/objectifs"
    val lenses = crawlCamaraWebsiteForm("${baseLensesPath}/pour-hybrides/focales-fixes") +
            crawlCamaraWebsiteForm("${baseLensesPath}/pour-hybrides/zooms") +
            crawlCamaraWebsiteForm("${baseLensesPath}/pour-hybrides/macro") +
            crawlCamaraWebsiteForm("${baseLensesPath}/pour-hybrides/dedies-video") +
            crawlCamaraWebsiteForm("${baseLensesPath}/pour-moyen-format/focales-fixes") +
            crawlCamaraWebsiteForm("${baseLensesPath}/pour-moyen-format/zooms") +
            crawlCamaraWebsiteForm("${baseLensesPath}/pour-moyen-format/macro")

    return lenses
        .mapNotNull { (key, deal) ->
            val sanitizedName = key.removePrefix("FUJIFILM ").toUpperCase()
            val characteristics = lensCharacteristicsFromName(sanitizedName)
                ?.let { inferClosestCharacteristics(it, fujiLensesCharacteristics) }
            val id = characteristics?.normalizedId()
            if (id != null) id to deal else null
        }
        .groupBy({ it.first }, { it.second })
}

fun crawlCamaraWebsiteForm(path: String): List<Pair<String, ProductNewDeal>> {
    val document = Jsoup.connect(path).userAgent(USER_AGENT).get()
    val form = document.select("form[action=$path]")

    val fields = form.select("select,input")

    val formData = fields.map { f ->
        f.attr("name") to
                if (f.tagName() == "select" && f.id() == "select_filter_marques") {
                    val fujiOption = f.select("option").filter { it.text() == "FUJIFILM" }[0]!!
                    fujiOption.attr("value")
                } else if (f.tagName() == "select" && f.id() == "limit_hikashop_category_information_module_87_8") {
                    "0"
                } else if (f.tagName() == "select") {
                    val selectedOption = f.select("option").filter { it.attr("selected") == "selected" }[0]!!
                    selectedOption.attr("value")
                } else {
                    f.attr("value")
                }
    }.toMap()

    return crawlCamaraWebsiteResults(path, formData)
}

fun crawlCamaraWebsiteResults(path: String, formData: Map<String, String>): List<Pair<String, ProductNewDeal>> {
    val content = Jsoup.connect(path).userAgent(USER_AGENT)
        .data(formData)
        .post()
    val products = content.select(".hikashop_product")
    return products.map { p ->
        val link = p.select(".hikashop_product_name")[0]!!.child(0)
        val url = link.attr("href")
        val name = link.text().trim()
        val price = parseCamaraPrice(p.select(".camara_price_wrapper")[0]!!.text())
        name to ProductNewDeal("Camara", price, "$camaraWebsite/$url")
    }
}

private fun sanitizeCamaraCameraName(titleText: String) =
    camaraCameraNameSanitizer.sanitize(titleText.toLowerCase()).trim()

val camaraCameraNameSanitizer = StringSanitizer(
    // Remove Fuji brand names
    "fujifilm" to { "" },
)

private fun parseCamaraPrice(priceText: String) =
    priceText.trim().removeSuffix(" € TTC").replace(',', '.').toDouble()

val camaraLensRegex = Regex("^([A-Z]*) ?([0-9-]*) ?(MM)? ?([FT])?/?([0-9,.-]*) ?([A-Z ]*)?([+].*)?$")

fun lensCharacteristicsFromName(name: String): LensCharacteristics? {
    val result = camaraLensRegex.matchEntire(name) ?: return null
    val groups = result.groupValues
    val series = groups[1]
    val focalLengths = groups[2].split("-").map { it.toInt() }
    val stopType = groups[4].firstOrNull() ?: 'F'
    val stops = groups[5].split("-").map {
        round(it.replace(',', '.').toFloat() * 10.0f) / 10.0f
    }
    val features = groups[6].split(" ").filter { !it.isColorAttribute() && !it.isShapeAttribute() }.toSet()
    return LensCharacteristics(
        series,
        FocalLength(focalLengths[0], if (focalLengths.size == 2) focalLengths[1] else null),
        Stops(stopType, stops[0], if (stops.size == 2) stops[1] else null),
        features
    )
}

val colorAttributes = listOf("argent", "noir")
val shapeAttributes = listOf("pancake")

private fun String.isColorAttribute(): Boolean {
    return colorAttributes.contains(this.toLowerCase())
}

private fun String.isShapeAttribute(): Boolean {
    return shapeAttributes.contains(this.toLowerCase())
}

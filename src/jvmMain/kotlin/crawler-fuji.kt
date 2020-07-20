package com.villevalois.fuji

import org.jsoup.Jsoup
import kotlin.math.round

const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20100101 Firefox/10.0"

fun crawlFujiCameras(): List<Camera> {
    return crawlFujiProducts("cameras") { id, name, imageUrl ->
        Camera(id, name.removePrefix("FUJIFILM "), imageUrl)
    }
}

fun crawlFujiLenses(): List<Lens> {
    return crawlFujiProducts("lenses") { id, name, imageUrl ->
        val characteristics = lensCharacteristicsFromFujiProductName(name)
//        println("$name => $characteristics")
        Lens(
            characteristics?.normalizedId() ?: id,
            characteristics?.normalizedName() ?: name,
            imageUrl,
            characteristics
        )
    }
}

private fun <P : Product> crawlFujiProducts(
    category: String,
    builder: (id: String, name: String, imageUrl: String) -> P,
): List<P> {
    val baseProductListPath = "${Product.baseProductPagePath}/$category/"
    val document = Jsoup.connect(baseProductListPath).userAgent(USER_AGENT).get()

    return document.select("ul.products__series_list a").map {
        val productPage = it.attributes()["href"]
        val id = productPage.removePrefix(baseProductListPath).removeSuffix("/")
        val imageUrl = it.child(0).attributes()["src"]
        val name = it.child(1).text()
        builder(id, name, imageUrl)
    }.distinctBy { it.productPagePath }
}

fun LensCharacteristics.normalizedId() =
    "$series ${focalLength}mm $stops${features.sorted().joinToString(" ", " ")}"

fun LensCharacteristics.normalizedName(): String =
    "${this.series} ${this.focalLength}mm ${this.stops}${this.features.joinToString(" ", " ")}"

val fujiLensRegex = Regex("^([A-Z]*)([0-9.-]*)mm(F|T)([0-9.-]*) ?([a-zA-Z ]*)?$")

fun lensCharacteristicsFromFujiProductName(name: String): LensCharacteristics? {
    val result = fujiLensRegex.matchEntire(name) ?: return null
    val groups = result.groupValues
    val series = groups[1]
    val focalLengths = groups[2].split("-").map { it.toInt() }
    val stopType = groups[3].first()
    val stops = groups[4].split("-").map { round(it.toFloat() * 10.0f) / 10.0f }
    val features = if (groups.size > 5) groups[5].split(" ").toSet() else emptySet()
    return LensCharacteristics(
        series,
        FocalLength(focalLengths[0], if (focalLengths.size == 2) focalLengths[1] else null),
        Stops(stopType, stops[0], if (stops.size == 2) stops[1] else null),
        features
    )
}

fun inferClosestCharacteristics(
    characteristics: LensCharacteristics,
    candidates: List<LensCharacteristics>,
): LensCharacteristics? {
    var filteredCandidates = candidates
        .filter { it.series == characteristics.series }
        .filter { it.focalLength == characteristics.focalLength }
        .filter { it.stops == characteristics.stops }
    if (filteredCandidates.size == 1) return filteredCandidates[0]

    for (feature in characteristics.features) {
        filteredCandidates = filteredCandidates.filter { it.features.contains(feature) }
        if (filteredCandidates.size == 1) return filteredCandidates[0]
    }

    if (filteredCandidates.isEmpty()) return null
    return filteredCandidates.sortedBy { it.features.size }[0]
}

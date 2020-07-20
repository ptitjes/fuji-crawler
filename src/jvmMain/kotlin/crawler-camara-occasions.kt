package com.villevalois.fuji

import com.villevalois.fuji.utils.StringSanitizer
import org.jsoup.Jsoup

const val camaraAdsWebsite = "http://www.camaraoccasions.net"
const val camaraAdsPage = "$camaraAdsWebsite/petites-annonces/petites-annonces-france-0-0-0-0.html"

fun crawlCamaraCamerasAds(): Map<String, List<ProductSecondHandDeal>> {
    val formData = mapOf(
        "SelCat" to "2634",
        "FicParam" to "APN.kp5",
        "zVar1" to "Fujifilm",
    ) + listOf("LibSearch", "REG2", "RefREG2", "DEP2", "Ou2", "CP2").map { it to "" }.toMap()

    val ads = crawlCamaraAds(formData)
    return ads
        .map { (name, deal) -> sanitizeCamaraAdsCameraName(name) to deal }
        .groupBy({ it.first }, { it.second })
}

fun crawlCamaraLensesAds(): Map<String, List<ProductSecondHandDeal>> {
    val fujiLensesCharacteristics = crawlFujiLenses().mapNotNull { it.characteristics }

    val formData = mapOf(
        "SelCat" to "2645",
        "FicParam" to "Optiques.kp5",
        "zVar1" to "Fujifilm",
        "zVar2" to "",
    ) + listOf("LibSearch", "REG2", "RefREG2", "DEP2", "Ou2", "CP2").map { it to "" }.toMap()

    val ads = crawlCamaraAds(formData)
    return ads
        .mapNotNull { (name, deal) ->
            val sanitizedName = sanitizeAdsLensName(name.toUpperCase())
            val characteristics = lensCharacteristicsFromName(sanitizedName)
                ?.let { inferClosestCharacteristics(it, fujiLensesCharacteristics) }
            val id = characteristics?.normalizedId()
            if (id != null) id to deal else null
        }
        .groupBy({ it.first }, { it.second })
}

private fun crawlCamaraAds(formData: Map<String, String>): List<Pair<String, ProductSecondHandDeal>> {
    val content = Jsoup.connect(camaraAdsPage).userAgent(USER_AGENT).data(formData).post();

    val ads = content.select(".ListTab tr").map { ad ->
        val link = ad.select(".ListLienTitre")
        val name = link.text()
        val price = parseCamaraAdsPrice(ad.select(".ListPrix").text())
        val url = "$camaraAdsWebsite${link.attr("href")}"
        val title = sanitizeCamaraAdsTitle(link.text())
        val imageUrl = "$camaraAdsWebsite${ad.select(".ListImg a img").first().attr("src")}"
        val location = ad.select("a").takeLast(2).joinToString(" / ") { it.text() }
        val publicationDate = ad.select(".ListCol1").text().trim()
        name to ProductSecondHandDeal("Camara", price, url, title, imageUrl, location, publicationDate)
    }
    return ads
}

private fun sanitizeCamaraAdsTitle(titleText: String) =
    camaraAdsTitleSanitizer.sanitize(titleText)

val camaraAdsTitleSanitizer = StringSanitizer(
    // Remove Fuji brand names
    "(fujifilm|Fujifilm|FUJIFILM)" to { "" },
    "(fuji|Fuji|FUJI)" to { "" },
)

private fun sanitizeCamaraAdsCameraName(titleText: String) =
    camaraAdsCameraNameSanitizer.sanitize(titleText.toLowerCase())

val camaraAdsCameraNameSanitizer = StringSanitizer(
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

fun sanitizeAdsLensName(titleText: String) =
    camaraAdsLensNameSanitizer.sanitize(titleText)

val camaraAdsLensNameSanitizer = StringSanitizer(
    // Remove Fuji brand names and useless terms
    "FUJIFILM|FUJI" to { "" },
    "SUPER|EBC|OBJECTIF|ZOOM" to { "" },
    // Clean Lenses names
    "(.*)XF(.*)" to { "XF ${it.groupValues[1]}${it.groupValues[2]}" }
)

private fun parseCamaraAdsPrice(priceText: String) =
    priceText.removeSuffix(" \u0080").replace(" ", "").toDouble()

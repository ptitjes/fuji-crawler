package com.villevalois.fuji

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class Crawler {
    suspend fun startUpdates() = coroutineScope {

    }
}

fun main() {
//    crawlImagesPhotoCameraAds()
    runBlocking {
        crawlImagesPhotoCameraAds().apply { listProducts() }
        crawlImagesPhotoLensesAds().apply { listProducts() }
    }
}

private fun Map<String, List<ProductSecondHandDeal>>.listProducts() {
    forEach { name, deals ->
        println("$name:")
        for (deal in deals) {
            println("  $deal")
        }
    }
}

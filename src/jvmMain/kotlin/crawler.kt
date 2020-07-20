package com.villevalois.fuji

import kotlinx.coroutines.runBlocking

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

package com.villevalois.fuji

import kotlinx.serialization.Serializable

@Serializable
sealed class Product {

    abstract val category: String
    abstract val id: String
    abstract val name: String
    abstract val imageUrl: String

    val productPagePath: String get() = "$baseProductPagePath/$category/$id"

    companion object {
        const val baseProductPagePath = "https://fujifilm-x.com/fr-fr/products"
    }
}

@Serializable
data class Camera(
    override val id: String,
    override val name: String,
    override val imageUrl: String,
) : Product() {
    override val category get() = "cameras"
}

@Serializable
data class Lens(
    override val id: String,
    override val name: String,
    override val imageUrl: String,
    val characteristics: LensCharacteristics?,
) : Product() {
    override val category get() = "lenses"
}

@Serializable
data class LensCharacteristics(
    val series: String,
    val focalLength: FocalLength,
    val stops: Stops,
    val features: Set<String>,
) {
    val shortId get() = "$series ${focalLength}mm $stops"
}

@Serializable
data class FocalLength(val min: Int, val max: Int?) {
    override fun toString(): String =
        "$min" + (if (max == null) "" else "-$max")
}

@Serializable
data class Stops(val type: Char, val min: Float, val max: Float?) {
    override fun toString(): String =
        "$type$min" + if (max == null) "" else "-$max"
}

@Serializable
sealed class ProductDeal {
    abstract val source: String
    abstract val price: Double
    abstract val url: String
    abstract val title: String?
}

@Serializable
data class ProductNewDeal(
    override val source: String,
    override val price: Double,
    override val url: String,
    override val title: String? = null,
) : ProductDeal()

@Serializable
data class ProductSecondHandDeal(
    override val source: String,
    override val price: Double,
    override val url: String,
    override val title: String,
    val imageUrl: String,
    val location: String? = null,
    val publicationDate: String? = null,
) : ProductDeal()

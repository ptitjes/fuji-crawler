package com.villevalois.fuji.frontend

import com.villevalois.fuji.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.css.properties.TextDecoration
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import react.*
import react.dom.a
import react.dom.input
import react.dom.label
import styled.*

val scope = MainScope()

val App = functionalComponent<RProps> {
    styledDiv {
        css {
            fontFamily = "helvetica, sans-serif"
            backgroundColor = Color("#fafafa")
        }

        child(CamerasPanel)
        child(LensesPanel)
    }
}

typealias CameraFilter = (Camera) -> Boolean

object CameraFilters {
    private fun nameFilter(predicate: (String) -> Boolean): CameraFilter =
        { predicate(it.name) }

    val XMount = nameFilter { it.startsWith("X-") }
    val GMount = nameFilter { it.startsWith("GFX") }
    val Compact = nameFilter { it.startsWith("X") && !it.startsWith("X-") && !it.startsWith("XP") }
    val FinePix = nameFilter { it.startsWith("XP") }
}

val CamerasPanel = functionalComponent<RProps> {
    child(ProductsPanel<Camera>()) {
        attrs.category = "Cameras"
        attrs.availableFilters = listOf(
            Pair("G-Mount", CameraFilters.GMount),
            Pair("X-Mount", CameraFilters.XMount),
            Pair("Compact", CameraFilters.Compact),
            Pair("FinePix", CameraFilters.FinePix),
        )
        attrs.initialFilters = setOf(CameraFilters.XMount)

        attrs.productLoader = { getCameras() }
        attrs.dealsLoader = { getCamaraCamerasDeals() }
        attrs.ads = getCameraAds()
    }
}

typealias LensFilter = (Lens) -> Boolean

object LensFilters {
    private fun characteristicsFilter(predicate: (LensCharacteristics) -> Boolean): LensFilter =
        { it.characteristics?.let { c -> predicate(c) } ?: false }

    private fun seriesFilter(predicate: (String) -> Boolean) = characteristicsFilter { predicate(it.series) }
    private fun featureFilter(predicate: (Set<String>) -> Boolean) = characteristicsFilter { predicate(it.features) }

    val GMount = seriesFilter { it.startsWith("GF") }
    val XMount = seriesFilter { it.startsWith("X") || it.endsWith("X") }

    val Prime: LensFilter = characteristicsFilter { it.focalLength.max == null }
    val Zoom: LensFilter = characteristicsFilter { it.focalLength.max != null }

    val R = featureFilter { it.contains("R") }
    val WR = featureFilter { it.contains("WR") }
    val LM = featureFilter { it.contains("LM") }
    val OIS = featureFilter { it.contains("OIS") }
}

val LensesPanel = functionalComponent<RProps> {
    child(ProductsPanel<Lens>()) {
        attrs.category = "Lenses"
        attrs.availableFilters = listOf(
            Pair("G-Mount", LensFilters.GMount),
            Pair("X-Mount", LensFilters.XMount),
            Pair("Prime", LensFilters.Prime),
            Pair("Zoom", LensFilters.Zoom),
            Pair("R", LensFilters.R),
            Pair("WR", LensFilters.WR),
            Pair("LM", LensFilters.LM),
            Pair("OIS", LensFilters.OIS),
        )
        attrs.initialFilters = setOf(LensFilters.XMount)

        attrs.productLoader = { getLenses() }
        attrs.dealsLoader = { getCamaraLensesDeals() }
        attrs.ads = getLensesAds()
    }
}

interface ProductsPanelProps<P : Product> : RProps {
    var category: String
    var availableFilters: List<Pair<String, (P) -> Boolean>>
    var initialFilters: Set<(P) -> Boolean>

    var productLoader: suspend () -> List<P>
    var dealsLoader: suspend () -> Map<String, List<ProductDeal>>
    var ads: Flow<Map<String, List<ProductDeal>>>
}

@Suppress("FunctionName")
fun <P : Product> ProductsPanel() = functionalComponent<ProductsPanelProps<P>> { props ->
    val (products, setProducts) = useState<List<P>?>(null)
    val (deals, setDeals) = useState<Map<String, List<ProductDeal>>?>(null)
    val (ads, setAds) = useState<Map<String, List<ProductDeal>>?>(null)
    val (filters, setFilters) = useState(props.initialFilters)

    useEffect(dependencies = listOf()) {
        scope.launch {
            setProducts(props.productLoader())
        }
        scope.launch {
            setDeals(props.dealsLoader())
        }
        scope.launch {
            props.ads.collect { setAds(it) }
        }
    }

    styledDiv {
        css {
            display = Display.flex
            flexDirection = FlexDirection.row
            alignItems = Align.baseline
            backgroundColor = Color.black
            color = Color.white
            padding(5.px)
        }
        styledSpan {
            css {
                padding(5.px)
                fontSize = 1.5.em
                fontWeight = FontWeight.bold
            }
            +props.category
        }

        props.availableFilters.forEach { (name, filter) ->
            val id = "${props.category}-$name"
            styledSpan {
                key = id
                css { padding(5.px) }
                input(type = InputType.checkBox, name = name) {
                    attrs.id = id
                    attrs.checked = filters.contains(filter)
                    attrs.onChangeFunction = {
                        setFilters(if (filters.contains(filter)) filters - filter else filters + filter)
                    }
                }
                label {
                    attrs.htmlFor = id
                    +name
                }
            }
        }
    }

    if (products != null) {
        val filteredProducts = products.filter { product ->
            filters.fold(true) { acc, filter ->
                acc && filter(product)
            }
        }

        styledUl {
            css { +ProductStyles.productGrid }

            filteredProducts.forEach { product ->
                styledLi {
                    key = product.id
                    css { +ProductStyles.productBox }

                    child(ProductCard) {
                        attrs.product = product
                    }
                    child(DealsPanel) {
                        attrs.deals = deals?.let { it[product.id] ?: emptyList() }
                    }
                    child(DealsPanel) {
                        attrs.deals = ads?.let { it[product.id] ?: emptyList() }
                    }
                }
            }
        }
    } else {
        child(Loading)
    }
}

external interface ProductCardProps : RProps {
    var product: Product
}

val ProductCard = functionalComponent<ProductCardProps> { props ->
    val product = props.product

    a(href = product.productPagePath, target = "_blank") {
        styledP {
            css { +ProductStyles.productName }
            +product.name
        }
        styledImg(product.name, product.imageUrl) {
            css { +ProductStyles.productThumbnail }
        }
    }
}

interface DealsPanelProps : RProps {
    var deals: List<ProductDeal>?
}

val DealsPanel = functionalComponent<DealsPanelProps> { props ->
    val deals = props.deals?.sortedBy { it.price }

    if (deals != null) {
        styledUl {
            css { +ProductStyles.productDeals }
            deals.forEach { deal -> child(DealRow) { attrs.deal = deal } }
        }
    } else {
        child(Loading)
    }
}

interface DealViewProps<D : ProductDeal> : RProps {
    var deal: D
}

val DealRow = functionalComponent<DealViewProps<ProductDeal>> { props ->
    val deal = props.deal

    styledLi {
        css {
            +ProductStyles.productDeal
            +if (deal is ProductNewDeal) ProductStyles.newProductDeal else ProductStyles.usedProductDeal
            +ProductStyles.tooltip
        }
        styledA(href = deal.url, target = "_blank") {
            css {
                width = 100.pct
                textDecoration = TextDecoration.none
            }
            styledP {
                css {
                    +ProductStyles.productDealContent
                }
                styledImg(deal.source, logoUrlBySource[deal.source]) {
                    css {
                        height = 20.px
                        marginRight = 10.px
                    }
                }

                val title = deal.title

                styledSpan {
                    css { +ProductStyles.productDealDescription }
                    +(if (deal is ProductNewDeal) "(New)" else title ?: "")
                }
                styledSpan {
                    css { +ProductStyles.productDealPrice }
                    +deal.price.format(2)
                    +" €"
                }

                styledSpan {
                    css { +ProductStyles.tooltipText }
                    if (deal is ProductNewDeal) child(NewDealDetails) { attrs.deal = deal }
                    else if (deal is ProductSecondHandDeal) child(SecondHandDealDetails) { attrs.deal = deal }
                }
            }
        }
    }
}

val logoUrlBySource = mapOf(
    "Camara" to "https://www.camara.net/templates/sapc/img/logocamaraonly.png",
    "Images Photo" to "https://www.images-photo.com/img/images-photo-devlocal-logo-1523260826.jpg",
)

val NewDealDetails = functionalComponent<DealViewProps<ProductNewDeal>> { props ->
    val deal = props.deal
}

val SecondHandDealDetails = functionalComponent<DealViewProps<ProductSecondHandDeal>> { props ->
    val deal = props.deal

    styledDiv {
        css {
            minWidth = 400.px
            display = Display.flex
            flexDirection = FlexDirection.column
        }
        styledP {
            +deal.title
        }
        styledDiv {
            css {
                display = Display.flex
                flexDirection = FlexDirection.row
            }
            if (deal.location != null) styledDiv {
                css { flexGrow = 1.0 }
                +deal.location
            }
            styledDiv {
                +deal.price.format(2)
                +" €"
            }
        }
        styledDiv {
            styledImg {
                attrs.src = deal.imageUrl
                css { width = 100.pct }
            }
        }
    }
}

val Loading = functionalComponent<RProps> {
    styledImg(
        "Loading",
        "https://www.voya.ie/Interface/Icons/LoadingBasketContents.gif"
    ) {
        css {
            display = Display.block
            height = 32.px
            width = 32.px
            marginLeft = LinearDimension.auto
            marginRight = LinearDimension.auto
        }
    }
}

fun Double.format(digits: Int): String = this.asDynamic().toFixed(digits) as String

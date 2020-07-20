package com.villevalois.fuji.frontend

import kotlinx.css.*
import kotlinx.css.borderRight
import kotlinx.css.properties.*
import styled.StyleSheet

object ProductStyles : StyleSheet("ProductStyles", isStatic = true) {
    val productGrid by css {
        display = Display.grid
        gridTemplateColumns = GridTemplateColumns.repeat("auto-fill, minmax(300px, 1fr)")
        gridAutoFlow = GridAutoFlow.row
        borderTop(1.px, BorderStyle.solid, Color.grey)
        borderLeft(1.px, BorderStyle.solid, Color.grey)
        padding(0.px)
    }

    val productBox by css {
        display = Display.flex
        flexDirection = FlexDirection.column
        backgroundColor = Color("#fafafa")
        borderBottom(1.px, BorderStyle.solid, Color.grey)
        borderRight(1.px, BorderStyle.solid, Color.grey)
        position = Position.relative
    }

    val productName by css {
        position = Position.absolute
        top = 0.px
        left = 0.px
        right = 0.px
        textAlign = TextAlign.center
        fontWeight = FontWeight.bold
        margin(0.px)
        padding(5.px)
        borderBottom(1.px, BorderStyle.solid, Color.grey)
        backgroundColor = Color.grey
        color = Color("#fafafa")
    }

    val productThumbnail by css {
        width = LinearDimension("calc(100% - 1px)")
    }

    val productDeals by css {
        padding(0.px)
        display = Display.flex
        flexDirection = FlexDirection.column
    }

    val productDeal by css {
        padding(2.px)
        display = Display.flex
        flexDirection = FlexDirection.row
    }

    val newProductDeal by css {
        backgroundColor = Color.darkGrey
    }

    val usedProductDeal by css {
        backgroundColor = Color.lightGrey
    }

    val productDealContent by css {
        display = Display.flex
        flexDirection = FlexDirection.row
        width = LinearDimension.auto
        margin(0.px)
        padding(5.px)
    }

    val productDealDescription by css {
        flexGrow = 1.0
        display = Display.block
        overflow = Overflow.hidden
        whiteSpace = WhiteSpace.nowrap
        textOverflow = TextOverflow.ellipsis
    }

    val productDealPrice by css {
        whiteSpace = WhiteSpace.nowrap
    }

    val tooltip by css {
        position = Position.relative
    }

    val tooltipText by css {
        visibility = Visibility.hidden
        backgroundColor = Color.black
        color = Color.white
        textAlign = TextAlign.center
        padding(5.px)
        borderRadius = 6.px
        minWidth = 250.px

        position = Position.absolute
        top = 100.pct
        zIndex = 1

        ancestorHover(".${ProductStyles.name}-${ProductStyles::tooltip.name}") {
            visibility = Visibility.visible
        }

        after {
            content = QuotedString(" ")
            position = Position.absolute
            bottom = 100.pct
            left = 50.pct
            marginLeft = (-5).px
            borderWidth = 5.px
            borderStyle = BorderStyle.solid
            borderColor = Color.transparent
            borderBottomColor = Color.black
        }
    }
}

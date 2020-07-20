package com.villevalois.fuji.frontend

import react.dom.render
import kotlinx.browser.document
import react.child

fun main() {
    render(document.getElementById("root")) {
        child(App) {}
    }
}

package com.villevalois.fuji.utils

class StringSanitizer(private val rules: List<Pair<Regex, (MatchResult) -> CharSequence>>) {

    constructor(vararg rules: Pair<String, (MatchResult) -> CharSequence>) : this(
        rules.toList().map { (regexString, transform) -> Regex(regexString) to transform }
    )

    fun sanitize(input: CharSequence) = rules.fold(input.cleanSpaces()) { i, (regex, transform) ->
        regex.replace(i, transform).cleanSpaces()
    }.trim()

    private val spaces = Regex("[ \t\n\r]+")
    private fun CharSequence.cleanSpaces() = spaces.replace(this, " ")
}

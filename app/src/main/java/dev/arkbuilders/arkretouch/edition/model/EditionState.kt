package dev.arkbuilders.arkretouch.edition.model

data class EditionState(
    val strokeSliderExpanded: Boolean
) {

    companion object {
        val DEFAULT = EditionState(
            strokeSliderExpanded = false
        )
    }
}
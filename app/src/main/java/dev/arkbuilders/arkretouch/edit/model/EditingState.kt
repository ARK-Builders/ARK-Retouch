package dev.arkbuilders.arkretouch.edit.model

data class EditingState(
    val strokeSliderExpanded: Boolean
) {

    companion object {
        val DEFAULT = EditingState(
            strokeSliderExpanded = false
        )
    }
}
package dev.arkbuilders.arkretouch.editing.model

import androidx.compose.ui.graphics.Color

data class EditingState(
    val strokeSliderExpanded: Boolean = false,
    val strokeWidth: Float = 5f,
    val menusVisible: Boolean = true,
    val showSavePathDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val showMoreOptionsPopup: Boolean = false,
    val imageSaved: Boolean = false,
    val isSavingImage: Boolean = false,
    val showEyeDropperHint: Boolean = false,
    val showConfirmClearDialog: Boolean = false,
    val isLoaded: Boolean = false,
    val exitConfirmed: Boolean = false,
    val bottomButtonsScrollIsAtStart: Boolean = false,
    val bottomButtonsScrollIsAtEnd: Boolean = false,
    val usedColors: List<Color> = listOf()
) {

    companion object {
        val DEFAULT = EditingState()
    }
}
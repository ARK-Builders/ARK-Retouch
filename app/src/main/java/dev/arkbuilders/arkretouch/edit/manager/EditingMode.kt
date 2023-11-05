package dev.arkbuilders.arkretouch.edit.manager

enum class EditingMode {
    IDLE,
    ROTATE,
    RESIZE,
    ERASE,
    CROP,
    BLUR,
    ZOOM,
    EYE_DROPPER,
    PAN;

    fun isColorMode(): Boolean = this !in listOf(
        ROTATE,
        RESIZE,
        CROP,
        ERASE,
        BLUR,
    )
}
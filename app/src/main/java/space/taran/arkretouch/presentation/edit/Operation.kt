package dev.arkbuilders.arkretouch.presentation.edit

interface Operation {
    fun apply()

    fun undo()

    fun redo()
}

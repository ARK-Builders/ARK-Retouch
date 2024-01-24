package dev.arkbuilders.arkretouch.editing.model

interface Operation {
    fun apply()

    fun undo()

    fun redo()
}
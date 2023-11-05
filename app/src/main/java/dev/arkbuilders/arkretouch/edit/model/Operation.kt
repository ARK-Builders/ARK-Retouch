package dev.arkbuilders.arkretouch.edit.model

interface Operation {
    fun apply()

    fun undo()

    fun redo()
}
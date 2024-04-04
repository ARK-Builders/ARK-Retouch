package dev.arkbuilders.arkretouch.editing

interface Operation {
    fun apply()

    fun undo()

    fun redo()
}
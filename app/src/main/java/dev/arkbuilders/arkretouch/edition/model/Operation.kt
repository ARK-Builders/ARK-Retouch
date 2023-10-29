package dev.arkbuilders.arkretouch.edition.model

interface Operation {
    fun apply()

    fun undo()

    fun redo()
}
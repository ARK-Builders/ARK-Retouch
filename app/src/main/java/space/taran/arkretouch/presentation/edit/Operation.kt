package space.taran.arkretouch.presentation.edit

interface Operation {
    fun apply(extraBlock: () -> Unit)

    fun undo()

    fun redo()
}

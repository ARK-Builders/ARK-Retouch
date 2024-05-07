package dev.arkbuilders.arkretouch.editing.draw

import dev.arkbuilders.arkretouch.editing.Operation
import dev.arkbuilders.arkretouch.editing.manager.EditManager

class DrawOperation(private val editManager: EditManager) : Operation {
    override fun apply() {}

    override fun undo() {
        editManager.apply {
            if (drawPaths.isNotEmpty()) {
                redoPaths.push(drawPaths.pop())
                updateRevised()
                return
            }
        }
    }

    override fun redo() {
        editManager.apply {
            if (redoPaths.isNotEmpty()) {
                drawPaths.push(redoPaths.pop())
                updateRevised()
                return
            }
        }
    }
}
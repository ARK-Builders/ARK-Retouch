package space.taran.arkretouch.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import space.taran.arkretouch.di.DIManager
import space.taran.arkretouch.presentation.drawing.EditManager
import java.io.IOException
import javax.inject.Inject

class Preferences @Inject constructor() {

    private val appContext: Context = DIManager.component.app()

    suspend fun persistUsedColors(editManager: EditManager) {
        withContext(Dispatchers.IO) {
            try {
                val writer = appContext.openFileOutput(
                    COLORS_STORAGE,
                    Context.MODE_PRIVATE
                ).bufferedWriter()
                editManager.oldColors.forEach { color ->
                    val line = color.value.toString()
                    writer.appendLine(line)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun readUsedColors(
        editManager: EditManager,
        initPaintColor: (EditManager) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val read = async(Dispatchers.IO) {
                    val reader = appContext.openFileInput(COLORS_STORAGE)
                        .bufferedReader()
                    editManager.clearOldColors()
                    reader.readLines().forEach { line ->
                        val color = Color(line.toULong())
                        editManager.addColor(color)
                    }
                }
                withContext(Dispatchers.Main) {
                    read.await()
                    initPaintColor(editManager)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val COLORS_STORAGE = "colors"
    }
}

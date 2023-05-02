package space.taran.arkretouch.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import space.taran.arkretouch.di.DIManager
import space.taran.arkretouch.presentation.drawing.EditManager
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject
import kotlin.text.Charsets.UTF_8

class Preferences @Inject constructor() {

    private val appContext: Context = DIManager.component.app()

    suspend fun persistUsedColors(editManager: EditManager) {
        withContext(Dispatchers.IO) {
            try {
                val colorsStorage = appContext.filesDir.resolve(COLORS_STORAGE)
                    .toPath()
                val lines = editManager.oldColors.map { color ->
                    color.value.toString()
                }
                Files.write(colorsStorage, lines, UTF_8)
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
                val colorsStorage = appContext.filesDir.resolve(COLORS_STORAGE)
                val read = async(Dispatchers.IO) {
                    if (colorsStorage.exists()) {
                        editManager.clearOldColors()
                        colorsStorage.readLines().forEach { line ->
                            val color = Color(line.toULong())
                            editManager.addColor(color)
                        }
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

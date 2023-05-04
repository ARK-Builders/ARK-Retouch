package space.taran.arkretouch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arkretouch.di.DIManager
import space.taran.arkretouch.presentation.drawing.EditManager
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.exists
import kotlin.text.Charsets.UTF_8

class Preferences @Inject constructor() {

    private val appContext = DIManager.component.app()

    suspend fun persistDefaults(editManager: EditManager) {
        withContext(Dispatchers.IO) {
            val defaultsStorage = appContext.filesDir.resolve(DEFAULTS_STORAGE)
                .toPath()
            val lines = listOf(
                "$BACKGROUND_COLOR$KEY_VALUE_SEPARATOR" +
                    "${editManager.backgroundColor.value.value}",
                "$RESOLUTION$KEY_VALUE_SEPARATOR" +
                    "${editManager.resolution.value.width}" +
                    "$RESOLUTION_DELIMITER${editManager.resolution.value.height}"
            )
            Files.write(defaultsStorage, lines, UTF_8)
        }
    }

    suspend fun readDefaults(updateDefaults: (Color, IntSize) -> Unit) {
        withContext(Dispatchers.IO) {
            val defaultsStorage = appContext.filesDir.resolve(DEFAULTS_STORAGE)
                .toPath()
            if (defaultsStorage.exists()) {
                val lines = Files.readAllLines(defaultsStorage)
                val colorValue = lines[0].split(KEY_VALUE_SEPARATOR)[1].toULong()
                val resolutionValues = lines[1].split(KEY_VALUE_SEPARATOR)[1]
                    .split(RESOLUTION_DELIMITER)
                val width = resolutionValues[0].toInt()
                val height = resolutionValues[1].toInt()
                val color = Color(colorValue)
                val resolution = IntSize(width, height)
                withContext(Dispatchers.Main) {
                    updateDefaults(color, resolution)
                }
            } else {
                updateDefaults(Color.White, IntSize.Zero)
            }
        }
    }

    companion object {
        private const val DEFAULTS_STORAGE = "defaults"
        private const val KEY_VALUE_SEPARATOR = ":"
        private const val RESOLUTION_DELIMITER = ","
        private const val BACKGROUND_COLOR = "backgroundColor"
        private const val RESOLUTION = "resolution"
    }
}

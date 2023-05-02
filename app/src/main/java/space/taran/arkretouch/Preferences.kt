package space.taran.arkretouch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arkretouch.di.DIManager
import space.taran.arkretouch.presentation.drawing.EditManager
import java.nio.file.Files
import javax.inject.Inject
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
                "$RESOLUTION$KEY_VALUE_SEPARATOR${editManager.resolution.width}" +
                    "$RESOLUTION_DELIMITER${editManager.resolution.height}"
            )
            Files.write(defaultsStorage, lines, UTF_8)
        }
    }

    fun readDefaults() {}

    companion object {
        private const val DEFAULTS_STORAGE = "defaults"
        private const val KEY_VALUE_SEPARATOR = ":"
        private const val RESOLUTION_DELIMITER = ","
        private const val BACKGROUND_COLOR = "backgroundColor"
        private const val RESOLUTION = "resolution"
    }
}

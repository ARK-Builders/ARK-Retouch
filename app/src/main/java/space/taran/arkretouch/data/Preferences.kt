package space.taran.arkretouch.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.exists
import kotlin.text.Charsets.UTF_8

@Singleton
class Preferences @Inject constructor(private val appCtx: Context) {

    suspend fun persistUsedColors(
        colors: List<Color>
    ) = withContext(Dispatchers.IO) {
        try {
            val colorsStorage = appCtx.filesDir.resolve(COLORS_STORAGE)
                .toPath()
            val lines = colors.map { it.value.toString() }
            Files.write(colorsStorage, lines, UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun readUsedColors(): List<Color> {
        val colors = mutableListOf<Color>()
        withContext(Dispatchers.IO) {

            try {
                val colorsStorage = appCtx
                    .filesDir
                    .resolve(COLORS_STORAGE)
                    .toPath()

                if (colorsStorage.exists()) {
                    Files.readAllLines(colorsStorage, UTF_8).forEach { line ->
                        val color = Color(line.toULong())
                        colors.add(color)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return colors
    }

    companion object {
        private const val COLORS_STORAGE = "colors"
    }
}

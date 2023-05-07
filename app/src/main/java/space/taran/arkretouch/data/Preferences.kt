package space.taran.arkretouch.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.text.Charsets.UTF_8

@Singleton
class Preferences @Inject constructor(private val appCtx: Context) {

    suspend fun persistDefaults(color: Color, resolution: IntSize) {
        withContext(Dispatchers.IO) {
            val defaultsStorage = appCtx.filesDir.resolve(DEFAULTS_STORAGE)
                .toPath()
            val defaults = ImageDefaults(
                color.value,
                Resolution.fromIntSize(resolution)
            )
            val jsonString = defaults.toJson()
            defaultsStorage.writeText(jsonString, UTF_8)
        }
    }

    suspend fun readDefaults(): ImageDefaults {
        var defaults = ImageDefaults()
        try {
            withContext(Dispatchers.IO) {
                val defaultsStorage = appCtx.filesDir.resolve(DEFAULTS_STORAGE)
                    .toPath()
                if (defaultsStorage.exists()) {
                    val jsonString = defaultsStorage.readText(UTF_8)
                    defaults = ImageDefaults.fromJson(jsonString)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return defaults
    }

    companion object {
        private const val DEFAULTS_STORAGE = "defaults"
    }
}

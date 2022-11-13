package space.taran.arkretouch.presentation.edit

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.getstream.sketchbook.SketchbookController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.outputStream

class EditViewModel : ViewModel() {
    var strokeSliderExpanded by mutableStateOf(false)
    var menusVisible by mutableStateOf(true)
    var strokeWidth by mutableStateOf(5f)

    fun saveImage(savePath: Path, bitmap: ImageBitmap) =
        viewModelScope.launch(Dispatchers.IO) {
            savePath.outputStream().use { out ->
                bitmap.asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
}

class EditViewModelFactory @Inject constructor() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditViewModel() as T
    }
}
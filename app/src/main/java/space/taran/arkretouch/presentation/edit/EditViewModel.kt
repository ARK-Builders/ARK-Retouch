package space.taran.arkretouch.presentation.edit

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.io.path.outputStream

class EditViewModel(
    private val launchedFromIntent: Boolean,
) : ViewModel() {
    var strokeSliderExpanded by mutableStateOf(false)
    var menusVisible by mutableStateOf(true)
    var strokeWidth by mutableStateOf(5f)
    var showSavePathDialog by mutableStateOf(false)
    var showExitDialog by mutableStateOf(false)
    var closeApp by mutableStateOf(false)

    fun saveImage(savePath: Path, bitmap: ImageBitmap) =
        viewModelScope.launch(Dispatchers.IO) {
            savePath.outputStream().use { out ->
                bitmap.asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (launchedFromIntent)
                closeApp = true
        }
}

class EditViewModelFactory @AssistedInject constructor(
    @Assisted private val launchedFromIntent: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditViewModel(launchedFromIntent) as T
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted launchedFromIntent: Boolean,
        ): EditViewModelFactory
    }
}

package space.taran.arkretouch.presentation.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor

@Composable
fun ColorPickerDialog(
    isVisible: MutableState<Boolean>,
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
) {
    if (!isVisible.value) return
    var currentColor by remember { mutableStateOf(HsvColor.from(initialColor)) }
    Dialog(
        onDismissRequest = {
            isVisible.value = false
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, RoundedCornerShape(5))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ClassicColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                color = currentColor.toColor(),
                onColorChanged = {
                    currentColor = it
                }
            )
            TextButton(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                onClick = {
                    onColorChanged(currentColor.toColor())
                    isVisible.value = false
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(50.dp)
                            .border(
                                2.dp,
                                Color.LightGray,
                                CircleShape
                            )
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(color = currentColor.toColor())
                    )
                    Text(text = "Pick", fontSize = 18.sp)
                }
            }
        }
    }
}

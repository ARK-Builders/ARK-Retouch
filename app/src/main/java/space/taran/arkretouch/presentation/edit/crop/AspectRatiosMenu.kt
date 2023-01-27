package space.taran.arkretouch.presentation.edit.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import space.taran.arkretouch.R

@Composable
fun AspectRatiosMenu(isVisible: Boolean = false) {

    if (isVisible)
        Row(
            Modifier
                .fillMaxHeight()
                .background(Color.Red)
        ) {
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(46.dp)
                    .clickable {},
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_crop),
                contentDescription =
                stringResource(id = R.string.ark_retouch_crop_free),
                tint = MaterialTheme.colors.primary
            )
        }
}

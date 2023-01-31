package space.taran.arkretouch.presentation.edit.crop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import space.taran.arkretouch.R
import space.taran.arkretouch.presentation.edit.crop.AspectRatios.aspectRatios
import space.taran.arkretouch.presentation.edit.crop.AspectRatios.isCropFree
import space.taran.arkretouch.presentation.edit.crop.AspectRatios.isCropSquare
import space.taran.arkretouch.presentation.edit.crop.AspectRatios.isCrop_16_9
import space.taran.arkretouch.presentation.edit.crop.AspectRatios.isCrop_3_2
import space.taran.arkretouch.presentation.edit.crop.AspectRatios.isCrop_5_4

@Composable
fun CropAspectRatiosMenu(isVisible: Boolean = false) {

    if (isVisible)
        Row(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        switchAspectRatio(isCropFree)
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp, bottom = 5.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(30.dp),
                    imageVector =
                    ImageVector.vectorResource(id = R.drawable.ic_crop),
                    contentDescription =
                    stringResource(id = R.string.ark_retouch_crop_free),
                    tint = if (isCropFree.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
                Text(
                    text = stringResource(R.string.ark_retouch_crop_free),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    color = if (isCropFree.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        switchAspectRatio(isCropSquare)
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp, bottom = 5.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(30.dp),
                    imageVector =
                    ImageVector.vectorResource(id = R.drawable.ic_crop_square),
                    contentDescription =
                    stringResource(id = R.string.ark_retouch_crop_square),
                    tint = if (isCropSquare.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
                Text(
                    stringResource(R.string.ark_retouch_crop_square),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (isCropSquare.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        switchAspectRatio(isCrop_5_4)
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(
                            start = 12.dp, end = 12.dp,
                            top = 5.dp, bottom = 5.dp
                        )
                        .align(Alignment.CenterHorizontally)
                        .size(30.dp),
                    imageVector =
                    ImageVector.vectorResource(id = R.drawable.ic_crop_5_4),
                    contentDescription =
                    stringResource(id = R.string.ark_retouch_crop_5_4),
                    tint = if (isCrop_5_4.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
                Text(
                    stringResource(R.string.ark_retouch_crop_5_4),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (isCrop_5_4.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        switchAspectRatio(isCrop_16_9)
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(
                            start = 12.dp, end = 12.dp,
                            top = 5.dp, bottom = 5.dp
                        )
                        .align(Alignment.CenterHorizontally)
                        .size(30.dp),
                    imageVector =
                    ImageVector.vectorResource(id = R.drawable.ic_crop_16_9),
                    contentDescription =
                    stringResource(id = R.string.ark_retouch_crop_16_9),
                    tint = if (isCrop_16_9.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
                Text(
                    stringResource(R.string.ark_retouch_crop_16_9),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (isCrop_16_9.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
            }
            Column(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        switchAspectRatio(isCrop_3_2)
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(
                            start = 12.dp, end = 12.dp,
                            top = 5.dp, bottom = 5.dp
                        )
                        .align(Alignment.CenterHorizontally)
                        .size(30.dp),
                    imageVector =
                    ImageVector.vectorResource(id = R.drawable.ic_crop_3_2),
                    contentDescription =
                    stringResource(id = R.string.ark_retouch_crop_3_2),
                    tint = if (isCrop_3_2.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
                Text(
                    stringResource(R.string.ark_retouch_crop_3_2),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (isCrop_3_2.value)
                        MaterialTheme.colors.primary
                    else Color.Black
                )
            }
        }
    else switchAspectRatio(isCropFree)
}

internal fun switchAspectRatio(selected: MutableState<Boolean>) {
    selected.value = true
    aspectRatios.filter {
        it != selected
    }.forEach {
        it.value = false
    }
}

internal object AspectRatios {
    val isCropFree = mutableStateOf(true)
    val isCropSquare = mutableStateOf(false)
    val isCrop_5_4 = mutableStateOf(false)
    val isCrop_16_9 = mutableStateOf(false)
    val isCrop_3_2 = mutableStateOf(false)
    val aspectRatios = listOf(
        isCropFree,
        isCropSquare,
        isCrop_5_4,
        isCrop_16_9,
        isCrop_3_2
    )
    val CROP_FREE = Pair(0, 0)
    val CROP_SQUARE = Pair(1, 1)
    val CROP_5_4 = Pair(5, 4)
    val CROP_16_9 = Pair(16, 9)
    val CROP_3_2 = Pair(3, 2)
}

package dev.arkbuilders.arkretouch.utils

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.math.atan2

fun Path.findNotExistCopyName(name: Path): Path {
    val parent = this
    var filesCounter = 1

    fun formatNameWithCounter() =
        "${name.nameWithoutExtension}_$filesCounter.${name.extension}"

    var newPath = parent.resolve(formatNameWithCounter())

    while (newPath.exists()) {
        newPath = parent.resolve(formatNameWithCounter())
        filesCounter++
    }
    return newPath
}

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

typealias Degrees = Float

fun PointerEvent.calculateRotationFromOneFingerGesture(
    center: Offset
): Degrees {
    var angleDelta = 0.0
    changes.forEach { change ->
        if (change.pressed) {
            val currentPosition = change.position
            val prevPosition = change.previousPosition
            val prevOffset = prevPosition - center
            val currentOffset = currentPosition - center
            val prevAngle = atan2(
                prevOffset.y.toDouble(),
                prevOffset.x.toDouble()
            )
            val currentAngle = atan2(
                currentOffset.y.toDouble(),
                currentOffset.x.toDouble()
            )
            angleDelta = Math.toDegrees(currentAngle - prevAngle)
        }
    }
    return angleDelta.toFloat()
}

fun Context.toast(@StringRes stringId: Int) {
    Toast.makeText(this, getString(stringId), Toast.LENGTH_SHORT).show()
}
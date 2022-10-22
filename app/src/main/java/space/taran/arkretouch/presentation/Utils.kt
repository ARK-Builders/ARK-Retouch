package space.taran.arkretouch.presentation

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

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
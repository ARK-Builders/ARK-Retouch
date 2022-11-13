package space.taran.arkretouch.presentation

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import space.taran.arkretouch.BuildConfig
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

fun Context.askWritePermissions() = getActivity()?.apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val packageUri =
            Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        val intent =
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                packageUri
            )
        startActivityForResult(intent, 1)
    } else {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            2
        )
    }
}

fun Context.isWritePermGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
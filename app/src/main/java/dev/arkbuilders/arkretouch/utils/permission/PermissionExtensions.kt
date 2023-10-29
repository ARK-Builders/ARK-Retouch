package dev.arkbuilders.arkretouch.utils.permission

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import dev.arkbuilders.arkretouch.BuildConfig
import dev.arkbuilders.arkretouch.utils.getActivity

fun Context.requestWritePermissions() = getActivity()?.apply {
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

fun Context.isWritePermissionGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val checkSelfPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        checkSelfPermission == PackageManager.PERMISSION_GRANTED
    }
}
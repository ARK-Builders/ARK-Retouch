package dev.arkbuilders.arkretouch.utils.permission

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import dev.arkbuilders.arkretouch.BuildConfig

object PermissionsHelper {
    fun writePermContract(): ActivityResultContract<String, Boolean> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AllFilesAccessContract()
        } else {
            ActivityResultContracts.RequestPermission()
        }
    }

    fun launchWritePerm(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val packageUri = "package:" + BuildConfig.APPLICATION_ID
            launcher.launch(packageUri)
        } else {
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
}
package dev.arkbuilders.arkretouch.utils.permission

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
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
}
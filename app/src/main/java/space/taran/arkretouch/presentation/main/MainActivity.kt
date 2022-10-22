package space.taran.arkretouch.presentation.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import space.taran.arkretouch.BuildConfig.APPLICATION_ID
import space.taran.arkretouch.presentation.edit.EditScreen
import space.taran.arkretouch.presentation.picker.FilePickerScreen
import space.taran.arkretouch.presentation.theme.ARKRetouchTheme
import kotlin.io.path.Path

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isWritePermGranted())
            askWritePermissions()

        setContent {
            ARKRetouchTheme {
                MainScreen(supportFragmentManager)
            }
        }
    }
}

@Composable
fun MainScreen(fragmentManager: FragmentManager) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "new"
    ) {
        composable("new") {
            FilePickerScreen(
                fragmentManager,
                onNavigateToEdit = { path ->
                    val screen = path?.let {
                        "edit?path=$path"
                    } ?: "edit"
                    navController.navigate(screen)
                }
            )
        }
        composable(
            "edit?path={path}",
            arguments = listOf(navArgument("path") {
                type = NavType.StringType
                nullable = true
            })
        ) { entry ->
            EditScreen(
                entry.arguments?.getString("path")?.let { Path(it) },
                fragmentManager,
                navigateBack =  { navController.popBackStack() }
            )
        }
    }
}

fun Activity.askWritePermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val packageUri =
            Uri.parse("package:$APPLICATION_ID")
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
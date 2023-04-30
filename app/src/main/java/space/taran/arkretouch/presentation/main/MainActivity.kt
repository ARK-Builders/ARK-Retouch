package space.taran.arkretouch.presentation.main

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import space.taran.arkretouch.presentation.utils.PermissionsHelper
import space.taran.arkretouch.presentation.edit.EditScreen
import space.taran.arkretouch.presentation.utils.isWritePermGranted
import space.taran.arkretouch.presentation.picker.PickerScreen
import space.taran.arkretouch.presentation.theme.ARKRetouchTheme
import kotlin.io.path.Path

private const val REAL_PATH_KEY = "real_file_path_2"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ARKRetouchTheme {
                MainScreen(
                    supportFragmentManager,
                    uri = intent.data?.toString(),
                    realPath = intent.getStringExtra(REAL_PATH_KEY),
                    launchedFromIntent = intent.data != null,
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    fragmentManager: FragmentManager,
    uri: String?,
    realPath: String?,
    launchedFromIntent: Boolean = false,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var backgroundColor = Color.White
    var resolution = IntSize.Zero
    val startScreen =
        if ((uri != null || realPath != null) && context.isWritePermGranted())
            NavHelper.editRoute
        else
            NavHelper.pickerRoute

    val launcher = rememberLauncherForActivityResult(
        contract = PermissionsHelper.writePermContract()
    ) { isGranted ->
        if (!isGranted) return@rememberLauncherForActivityResult
        navController.navigate(
            NavHelper.parseEditArgs(realPath, uri, launchedFromIntent)
        )
    }

    SideEffect {
        if (!context.isWritePermGranted())
            PermissionsHelper.launchWritePerm(launcher)
    }

    NavHost(
        navController = navController,
        startDestination = startScreen
    ) {
        composable(NavHelper.pickerRoute) {
            PickerScreen(
                fragmentManager,
                onNavigateToEdit = { path, _backgroundColor, _resolution ->
                    backgroundColor = _backgroundColor
                    resolution = _resolution
                    navController.navigate(
                        NavHelper.parseEditArgs(
                            path?.toString(),
                            uri = null,
                            launchedFromIntent = false,
                        )
                    )
                },
            )
        }
        composable(
            route = NavHelper.editRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = realPath
                    nullable = true
                },
                navArgument("uri") {
                    type = NavType.StringType
                    defaultValue = uri
                    nullable = true
                },
                navArgument("launchedFromIntent") {
                    type = NavType.BoolType
                    defaultValue = launchedFromIntent
                },
            )
        ) { entry ->
            EditScreen(
                entry.arguments?.getString("path")?.let { Path(it) },
                entry.arguments?.getString("uri"),
                backgroundColor,
                resolution,
                fragmentManager,
                navigateBack = { navController.popBackStack() },
                entry.arguments?.getBoolean("launchedFromIntent")!!,
            )
        }
    }
}

private object NavHelper {
    const val editRoute =
        "edit?path={path}&uri={uri}&launchedFromIntent={launchedFromIntent}"

    const val pickerRoute = "picker"

    fun parseEditArgs(
        path: String?,
        uri: String?,
        launchedFromIntent: Boolean,
    ): String {
        val screen = if (path != null) {
            "edit?path=$path&launchedFromIntent=$launchedFromIntent"
        } else if (uri != null) {
            "edit?uri=$uri&launchedFromIntent=$launchedFromIntent"
        } else {
            "edit"
        }
        return screen
    }
}

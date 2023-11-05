package dev.arkbuilders.arkretouch.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.arkbuilders.arkretouch.edit.ui.main.EditScreen
import dev.arkbuilders.arkretouch.picker.PickerScreen
import dev.arkbuilders.arkretouch.storage.Resolution
import dev.arkbuilders.arkretouch.utils.permission.PermissionsHelper
import dev.arkbuilders.arkretouch.utils.permission.isWritePermissionGranted
import kotlin.io.path.Path

private const val EXTRA_PATH = "EXTRA_PATH"
private const val EXTRA_URI = "EXTRA_URI"
private const val EXTRA_LAUNCHED_FROM_INTENT = "EXTRA_LAUNCHED_FROM_INTENT"

@Composable
fun MainScreen(
    fragmentManager: FragmentManager,
    uri: String?,
    realPath: String?,
    launchedFromIntent: Boolean = false,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var maxResolution by remember { mutableStateOf(Resolution(0, 0)) }
    val startScreen =
        if ((uri != null || realPath != null) && context.isWritePermissionGranted())  {
            NavHelper.editRoute
        } else {
            NavHelper.pickerRoute
        }

    val launcher = rememberLauncherForActivityResult(
        contract = PermissionsHelper.writePermContract()
    ) { isGranted ->
        if (!isGranted) return@rememberLauncherForActivityResult
        if (launchedFromIntent) {
            val route = NavHelper.parseEditArgs(realPath, uri, true)
            navController.navigate(route)
        }
    }

    SideEffect {
        if (!context.isWritePermissionGranted()) {
            PermissionsHelper.launchWritePerm(launcher)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startScreen
    ) {
        composable(NavHelper.pickerRoute) {
            PickerScreen(
                fragmentManager,
                onNavigateToEdit = { path, resolution ->
                    maxResolution = resolution
                    val parsedRoute = NavHelper.parseEditArgs(
                        path = path?.toString(),
                        uri = null,
                        launchedFromIntent = false,
                    )
                    navController.navigate(parsedRoute)
                },
            )
        }
        composable(
            route = NavHelper.editRoute,
            arguments = listOf(
                navArgument(EXTRA_PATH) {
                    type = NavType.StringType
                    defaultValue = realPath
                    nullable = true
                },
                navArgument(EXTRA_URI) {
                    type = NavType.StringType
                    defaultValue = uri
                    nullable = true
                },
                navArgument(EXTRA_LAUNCHED_FROM_INTENT) {
                    type = NavType.BoolType
                    defaultValue = launchedFromIntent
                },
            )
        ) { entry ->
            EditScreen(
                imagePath = entry.arguments?.getString(EXTRA_PATH)?.let { Path(it) },
                imageUri = entry.arguments?.getString(EXTRA_URI),
                fragmentManager = fragmentManager,
                navigateBack = { navController.popBackStack() },
                launchedFromIntent = entry.arguments?.getBoolean(EXTRA_LAUNCHED_FROM_INTENT)!!,
                maxResolution = maxResolution
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
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
import dev.arkbuilders.arkretouch.editing.ui.main.EditScreen
import dev.arkbuilders.arkretouch.picker.PickerScreen
import dev.arkbuilders.arkretouch.storage.Resolution
import dev.arkbuilders.arkretouch.utils.permission.PermissionsHelper
import dev.arkbuilders.arkretouch.utils.permission.isWritePermissionGranted
import kotlin.io.path.Path

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
        if ((uri != null || realPath != null) && context.isWritePermissionGranted())
            NavHelper.editRoute
        else
            NavHelper.pickerRoute

    val launcher = rememberLauncherForActivityResult(
        contract = PermissionsHelper.writePermContract()
    ) { isGranted ->
        if (!isGranted) return@rememberLauncherForActivityResult
        if (launchedFromIntent) {
            navController.navigate(
                NavHelper.parseEditArgs(realPath, uri, true)
            )
        }
    }

    SideEffect {
        if (!context.isWritePermissionGranted())
            PermissionsHelper.launchWritePerm(launcher)
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
                fragmentManager,
                navigateBack = { navController.popBackStack() },
                entry.arguments?.getBoolean("launchedFromIntent")!!,
                maxResolution
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
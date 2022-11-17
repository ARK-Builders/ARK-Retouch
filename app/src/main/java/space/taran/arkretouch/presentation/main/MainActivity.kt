package space.taran.arkretouch.presentation.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import space.taran.arkretouch.presentation.askWritePermissions
import space.taran.arkretouch.presentation.edit.EditScreen
import space.taran.arkretouch.presentation.isWritePermGranted
import space.taran.arkretouch.presentation.picker.PickerScreen
import space.taran.arkretouch.presentation.theme.ARKRetouchTheme
import kotlin.io.path.Path

private const val REAL_PATH_KEY = "real_file_path_2"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isWritePermGranted())
            askWritePermissions()

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
    val navController = rememberNavController()
    val startScreen = if (uri != null || realPath != null)
        "edit?path={path}&uri={uri}&launchedFromIntent={launchedFromIntent}"
    else
        "picker"

    NavHost(
        navController = navController,
        startDestination = startScreen
    ) {
        composable("picker") {
            PickerScreen(
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
            "edit?path={path}&" +
                "uri={uri}&" +
                "launchedFromIntent={launchedFromIntent}",
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
                }
            )
        ) { entry ->
            EditScreen(
                entry.arguments?.getString("path")?.let { Path(it) },
                entry.arguments?.getString("uri"),
                fragmentManager,
                navigateBack = { navController.popBackStack() },
                entry.arguments?.getBoolean("launchedFromIntent")!!
            )
        }
    }
}

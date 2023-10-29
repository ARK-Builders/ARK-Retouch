package dev.arkbuilders.arkretouch.main

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dev.arkbuilders.arkretouch.ui.theme.ARKRetouchTheme

private const val REAL_PATH_KEY = "real_file_path_2"

class RootActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ARKRetouchTheme {
                MainScreen(
                    fragmentManager = supportFragmentManager,
                    uri = intent.data?.toString(),
                    realPath = intent.getStringExtra(REAL_PATH_KEY),
                    launchedFromIntent = intent.data != null,
                )
            }
        }
    }
}
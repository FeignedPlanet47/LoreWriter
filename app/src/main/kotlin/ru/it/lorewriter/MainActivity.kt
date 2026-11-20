package ru.it.lorewriter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import ru.it.lorewriter.ui.LoreWriterApp
import ru.it.lorewriter.ui.theme.LoreWriterTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingDeepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        pendingDeepLink = intent?.dataString

        setContent {
            LoreWriterTheme {
                LoreWriterApp(
                    deepLink = pendingDeepLink,
                    onDeepLinkConsumed = {
                        pendingDeepLink = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        pendingDeepLink = intent.dataString
    }
}
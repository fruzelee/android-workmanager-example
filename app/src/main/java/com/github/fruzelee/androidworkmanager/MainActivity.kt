package com.github.fruzelee.androidworkmanager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.github.fruzelee.androidworkmanager.ui.theme.AndroidWorkManagerExampleTheme

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager

    // instantiate the view model
    private val viewModel by viewModels<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        workManager = WorkManager.getInstance(applicationContext)

        setContent {
            AndroidWorkManagerExampleTheme {

            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // get the uri from the intent
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return

        // updateUncompressUri which is just the uri we get from the other app like google chrome
        viewModel.updateUncompressUri(uri)

        // define the work request to launch the worker

        val request = OneTimeWorkRequestBuilder<PhotoCompressionWorker>()
            .setInputData(
                workDataOf(
                    PhotoCompressionWorker.KEY_CONTENT_URI to uri.toString(),
                    PhotoCompressionWorker.KEY_COMPRESSION_THRESHOLD to 1024 * 20L
                )
            )
            .setConstraints(
                Constraints(
                    requiresStorageNotLow = true
                )
            )
            .build()
        workManager.enqueue(request)

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidWorkManagerExampleTheme {
        Greeting("Android")
    }
}
package com.github.fruzelee.androidworkmanager

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
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

                // listen to the worker result here
                val workerResult = viewModel.workId?.let { id ->
                    workManager.getWorkInfoByIdLiveData(id).observeAsState().value
                }

                LaunchedEffect(key1 = workerResult?.outputData) {
                    if (workerResult?.outputData != null) {
                        // retrieve the filePath that we passed to the successful result after our worker is running
                        val filePath = workerResult.outputData.getString(
                            PhotoCompressionWorker.KEY_RESULT_PATH
                        )

                        // reconstruct the bitmap out of that filePath so that we can read that file as a bitmap
                        filePath?.let {
                            val bitmap = BitmapFactory.decodeFile(it)
                            viewModel.updateCompressBitmap(bitmap)
                        }
                    }
                }

                // display the images

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    viewModel.uncompressedUri?.let {
                        Text(text = "Uncompressed photo:")
                        AsyncImage(model = it, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    viewModel.compressedBitmap?.let {
                        Text(text = "Compressed photo:")
                        Image(bitmap = it.asImageBitmap(), contentDescription = null)
                    }
                }
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

            // remove the constraint here to fix the bug of not working of the workmanager compression

            /*.setConstraints(
                Constraints(
                    requiresStorageNotLow = true
                )
            )*/
            .build()
        viewModel.updateWorkId(request.id) // allow us to listen for updates of that specific worker we have launch here with this request
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
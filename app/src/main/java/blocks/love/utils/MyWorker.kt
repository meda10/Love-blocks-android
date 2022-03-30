package blocks.love.utils

import android.content.Context
import android.util.Log

import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient

class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val fileDownloader by lazy { FileDownloader(OkHttpClient.Builder().build()) }

    override fun doWork(): Result {
        Log.d("FCM", "Running Worker")
        val url = inputData.getString("url")
        val name = inputData.getString("name")
        if (url != null && name != null) {
            downloadLoveProject(
                url.replace("localhost", "192.168.0.20"),
                name.replace(" ", "_"),
                applicationContext,
                fileDownloader
            )
        }
        return Result.success()
    }
}


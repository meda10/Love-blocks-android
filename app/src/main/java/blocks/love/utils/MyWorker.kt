package blocks.love.utils

import android.content.Context
import android.os.Environment
import android.util.Log

import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import java.io.File

class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val fileDownloader by lazy { FileDownloader(OkHttpClient.Builder().build()) }

    override fun doWork(): Result {
        Log.d("FCM", "Running Worker")
        val url = inputData.getString("url")
        val name = inputData.getString("name")
        if (url != null && name != null) {
            val fileName = name.replace(" ", "_")
            val loveFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + fileName + ".love"
            downloadLoveProject(
                url.replace("localhost", "192.168.0.20"),
                applicationContext,
                fileDownloader,
                loveFilePath
            )
        }
        return Result.success()
    }
}


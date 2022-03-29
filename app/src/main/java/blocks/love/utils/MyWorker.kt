package blocks.love.utils

import android.content.Context
import android.util.Log

import androidx.work.Worker
import androidx.work.WorkerParameters

class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "WORKER"
    }

    override fun doWork(): Result {
        Log.d(TAG, "Running Worker")

        when {
            inputData.getString("apk") != null -> {

            }
            inputData.getString("file") != null -> {

            }
        }

        return Result.success()
    }
}


package blocks.love.utils

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit


fun View.showSnackBar(msgId: Int, length: Int) {
    showSnackBar(context.getString(msgId), length)
}

fun View.showSnackBar(msg: String, length: Int) {
    showSnackBar(msg, length, null) {}
}

fun View.showSnackBar(msgId: Int, length: Int, actionMessageId: Int, action: (View) -> Unit) {
    showSnackBar(context.getString(msgId), length, context.getString(actionMessageId), action)
}

fun View.showSnackBar(msg: String, length: Int, actionMessage: CharSequence?, action: (View) -> Unit) {
    val snackBar = Snackbar.make(this, msg, length)
    if (actionMessage != null) {
        snackBar.setAction(actionMessage) {
            action(this)
        }.show()
    }
}

fun View.showDialog(msg: String, titleId: Int, context: Context) {
    showDialog(msg, context.getString(titleId), context)
}

fun View.showDialog(msgId: Int, titleId: Int, context: Context) {
    showDialog(context.getString(msgId), context.getString(titleId), context)
}

fun View.showDialog(msg: String, title: String, context: Context) {
    val dialogBuilder = AlertDialog.Builder(context)

    dialogBuilder.setMessage(msg)
        .setCancelable(true)
//        .setPositiveButton("Install") { dialog, _ -> dialog.dismiss() }
        .setNegativeButton("Close") { dialog, _ -> dialog.cancel() }

    val alert = dialogBuilder.create()
    alert.setTitle(title)
    alert.show()
}

fun View.showDialogInstall(msgId: Int, titleId: Int, context: Context, packageName: String) {
    val dialogBuilder = AlertDialog.Builder(context)

    dialogBuilder.setMessage(context.getString(msgId))
        .setCancelable(true)
        .setPositiveButton("Install") { _, _ ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (e: ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }
        .setNegativeButton("Close") { dialog, _ -> dialog.cancel() }

    val alert = dialogBuilder.create()
    alert.setTitle(context.getString(titleId))
    alert.show()
}

fun downloadLoveProject(url: String, fileName: String, context: Context, fileDownloader: FileDownloader) {
    val loveFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + fileName
    val loveFile = File(loveFilePath)
    if (loveFile.exists()) loveFile.delete()
    Log.d("DOWNLOAD", "destination: $loveFilePath")

    val disposable = fileDownloader.download(url, loveFile)
        .throttleFirst(2, TimeUnit.SECONDS)
        .toFlowable(BackpressureStrategy.LATEST)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            Log.d("DOWNLOAD", "$it% Downloaded")
            Toast.makeText(context, "$it% Downloaded", Toast.LENGTH_SHORT).show()

        }, {
            Log.d("DOWNLOAD", "$it% Error")
            Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()

        }, {
            Log.d("DOWNLOAD", "Project Downloaded")
            openLove2dApp(loveFilePath, context)
        })
}


fun openLove2dApp(loveFilePath: String, context: Context){
    val file = File(loveFilePath)
    val uri = FileProvider.getUriForFile(context, "blocks.love.fileProvider", file)
    Log.d("DOWNLOAD", "uri: $uri mime: application/x-love-game");

    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    intent.setDataAndType(uri, "application/x-love-game")
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
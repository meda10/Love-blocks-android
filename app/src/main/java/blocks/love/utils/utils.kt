package blocks.love.utils

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import com.google.android.material.snackbar.Snackbar


fun View.showSnackbar(msgId: Int, length: Int) {
    showSnackbar(context.getString(msgId), length)
}

fun View.showSnackbar(msg: String, length: Int) {
    showSnackbar(msg, length, null, {})
}

fun View.showSnackbar(msgId: Int, length: Int, actionMessageId: Int, action: (View) -> Unit) {
    showSnackbar(context.getString(msgId), length, context.getString(actionMessageId), action)
}

fun View.showSnackbar(msg: String, length: Int, actionMessage: CharSequence?, action: (View) -> Unit) {
    val snackbar = Snackbar.make(this, msg, length)
    if (actionMessage != null) {
        snackbar.setAction(actionMessage) {
            action(this)
        }.show()
    }
}

fun View.showDialog(msgId: Int, titleId: Int, context: Context) {
    showDialog(context.getString(msgId), context.getString(titleId), context)
}

fun View.showDialog(msg: String, title: String, context: Context) {
    val dialogBuilder = AlertDialog.Builder(context)

    dialogBuilder.setMessage(msg)
        .setCancelable(true)
//        .setPositiveButton("Install") { dialog, _ -> dialog.dismiss() }
        .setNegativeButton("Close") { dialog, id -> dialog.cancel() }

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
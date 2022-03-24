package blocks.love.utils

// Source: https://androidwave.com/download-and-install-apk-programmatically/

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import blocks.love.BuildConfig
import blocks.love.R
import java.io.File
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession


//import blocks.love.BuildConfig
//import blocks.love.R


class DownloadController(private val context: Context, private val url: String) {

    companion object {
        private const val FILE_NAME = "SampleDownloadApp.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""
    }

    fun enqueueDownload() {
        val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + FILE_NAME
        val destinationUri = Uri.parse("$FILE_BASE_PATH$destination")
        Log.d("APK", "uri: $destinationUri")

        val file = File(destination)
        if (file.exists()) file.delete()

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setMimeType(MIME_TYPE)
        request.setTitle(context.getString(R.string.title_file_download))
        request.setDescription(context.getString(R.string.downloading))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationUri(destinationUri)

//        HttpsURLConnection.setDefaultHostnameVerifier(NullHostNameVerifier()) //todo remove
//        val context = SSLContext.getInstance("TLS")
//        context.init(null, arrayOf<X509TrustManager>(NullX509TrustManager()), SecureRandom())
//        HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)


        showInstallOption(destination, destinationUri)
        // Enqueue a new download and same the referenceId
        downloadManager.enqueue(request)
        Toast.makeText(context, context.getString(R.string.downloading), Toast.LENGTH_LONG).show()
        Log.d("APK", "APK downloaded")
    }

    private fun showInstallOption(destination: String, uri: Uri) {

        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                        File(destination)
                    )
                    val install = Intent(Intent.ACTION_VIEW)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.data = contentUri
                    context.startActivity(install)
                    context.unregisterReceiver(this)
                    // finish()
                } else {
                    val install = Intent(Intent.ACTION_VIEW)
                    install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    install.setDataAndType(
                        uri,
                        APP_INSTALL_PATH
                    )
                    context.startActivity(install)
                    context.unregisterReceiver(this)
                    // finish()
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}

class NullHostNameVerifier : HostnameVerifier {
    override fun verify(hostname: String, session: SSLSession): Boolean = true
}
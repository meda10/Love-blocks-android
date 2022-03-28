package blocks.love

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import blocks.love.utils.DownloadController
import blocks.love.utils.showSnackbar
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import okhttp3.ResponseBody
import java.io.*

class MainActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    lateinit var recyclerAdapter: RecyclerAdapter
    lateinit var downloadController: DownloadController

    companion object {
        const val WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 10
        const val READ_EXTERNAL_STORAGE_PERMISSION_CODE = 20
        const val REQUEST_INSTALL_PACKAGES_PERMISSION_CODE = 30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerAdapter = RecyclerAdapter(this)
//        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recyclerAdapter

        getUserProjects()
        if (checkGooglePlayServices()) {

        } else {
            //You won't be able to send notifications to this device
            Log.w("PLAY", "Device doesn't have google play services")
        }
    }

    override fun onStart() {
        super.onStart()

        val projectBtn = findViewById<View>(R.id.projectBtn) as Button
        val view_btn = findViewById<View>(R.id.view_btn) as Button
        val btnDownload = findViewById<View>(R.id.btnDownload) as Button
        view_btn.text = "Sign Out"
        view_btn.setOnClickListener { signOutButton() }
        projectBtn.setOnClickListener { getUserProjects() }
//        btnDownload.setOnClickListener { downloadFile() }
    }

    // Signs Out user -> On button click
    private fun signOutButton() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener { goToAuthActivity() }
    }

    private fun goToAuthActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun getUserProjects(){
        val user = Firebase.auth.currentUser
        user?.getIdToken(true)?.addOnCompleteListener { getIDToken ->
            if (getIDToken.isSuccessful) {
                val idToken = getIDToken.result.token
                val projectData = ProjectsData( id_token = idToken!! )

                RestApiManager().getProjects(projectData) { responseData ->
                    if (responseData != null ) {
                        Log.d("PROJECTS", "OK")
                        recyclerAdapter.setProjectListItems(responseData)
                    } else {
                        Log.d("PROJECTS", "NULL")
                    }
                }
            } else {
                Log.d("PROJECTS", getIDToken.exception.toString())
            }
        }
    }

    fun writeFile(body: ResponseBody, name: String) {
        if(writeFilePermissions()) {
            try {
                val path = File(getExternalStorageDirectory().absolutePath + File.separator +
                        "Android" + File.separator + "data" + File.separator + "org.love2d.android" +
                        File.separator + "files" + File.separator + "games" + File.separator +
                        "lovegame" + File.separator)
                Log.d("DOWNLOAD", "directory path: $path")
                Log.d("DOWNLOAD", "Exists: " + path.exists())
                if (!path.exists()){
                    path.mkdirs()
                    Log.d("DOWNLOAD", "Create path: $path")
                }
                val file = File(path.absolutePath + File.separator + name)
                Log.d("DOWNLOAD", "file path: $file")

                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null
                try {
                    val fileReader = ByteArray(4096)
                    val fileSize = body.contentLength()
                    var fileSizeDownloaded: Long = 0
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(file)
                    while (true) {
                        val read = inputStream.read(fileReader)
                        if (read == -1) {
                            break
                        }
                        outputStream.write(fileReader, 0, read)
                        fileSizeDownloaded += read.toLong()
                        Log.d("DOWNLOAD", "file download: $fileSizeDownloaded of $fileSize")
                    }
                    outputStream.flush()

                    val dest = File(getExternalStorageDirectory().absolutePath + File.separator + "lovegame" + File.separator  + name)
                    file.copyRecursively(dest, true, onError = { _, exception ->
                        Log.d("DOWNLOAD", "file copy fail")
                        Log.d("DOWNLOAD", exception.toString())
                        OnErrorAction.SKIP
                    })
                } catch (e: IOException) {
                    //todo handle error
                    Log.e("DOWNLOAD", e.toString())
                    Log.d("DOWNLOAD", "file download was fail")
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            } catch (e: IOException) {
                //todo handle error
                Log.d("DOWNLOAD", "file download was Mega fail")
                Log.e("DOWNLOAD", e.toString())
            }
        }
    }

    private fun writeFilePermissions(): Boolean {
        when {
            SDK_INT >= Build.VERSION_CODES.R -> {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = Uri.fromParts("package", this.packageName, null)
                        startActivity(intent)
                        Log.d("DOWNLOAD", "Permission INTENT")
                    } catch (e: Exception) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        startActivity(intent)
                        return false
                    }
                    Log.d("DOWNLOAD", "Permission granted R")
                    return true
                } else {
                    Log.d("DOWNLOAD", "Permission IS Manager")
                    return true
                }
            }
            else -> {
                requestPermission(WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE, R.string.storage_access_required)
                Log.d("DOWNLOAD", "Permission granted")
                return ActivityCompat.checkSelfPermission(this@MainActivity, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED
            }
        }
    }

    fun requestPermission(permission: String, requestCode: Int, message: Int) {
        if (ActivityCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, permission)) {
                val mainLayout = findViewById<View>(R.id.mainLayout) as ConstraintLayout
                mainLayout.showSnackbar(message, Snackbar.LENGTH_INDEFINITE, R.string.ok) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
                }
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Write storage Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    val mainLayout = findViewById<View>(R.id.mainLayout) as ConstraintLayout
                    mainLayout.showSnackbar(R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
                }
            }
            READ_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Read storage Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Read storage Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_INSTALL_PACKAGES_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Install Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Install Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkGooglePlayServices(): Boolean {
        return if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            Log.e("PLAY", "Error update play services")
            val mainLayout = findViewById<View>(R.id.mainLayout) as ConstraintLayout
            mainLayout.showSnackbar(R.string.update_play_service, Snackbar.LENGTH_SHORT)
            false
        } else true
    }

    fun downloadAPK(url: String, fileName: String) {
        val url = "https://loveblocks.tk/storage/game.apk"
        downloadController = DownloadController(this, url)
        Log.d("APK", "APK url: $url")
        downloadController.enqueueDownload(fileName)
    }
}
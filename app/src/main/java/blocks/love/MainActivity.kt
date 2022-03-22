package blocks.love

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import okhttp3.ResponseBody
import java.io.*


class MainActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    lateinit var recyclerAdapter: RecyclerAdapter
    val WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 10
    val READ_EXTERNAL_STORAGE_PERMISSION_CODE = 20
    val MANAGE_EXTERNAL_STORAGE_PERMISSION_CODE = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerAdapter = RecyclerAdapter(this)
//        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recyclerAdapter

        getUserProjects()
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
            }
        }
    }

    fun writeFile(body: ResponseBody, name: String) {
        try {
            writeFilePermissions()
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

    private fun writeFilePermissions() {
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
                    }
                    Log.d("DOWNLOAD", "Permission granted R")
                } else {
                    Log.d("DOWNLOAD", "Permission IS Manager")
                }
            }
            else -> {
                checkPermission(READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_PERMISSION_CODE)
                checkPermission(WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE)
                Log.d("DOWNLOAD", "Permission granted")
            }
        }
    }

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ActivityCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Write storage Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Write storage Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            MANAGE_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Manage storage Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Manage storage Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            READ_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Read storage Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Read storage Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
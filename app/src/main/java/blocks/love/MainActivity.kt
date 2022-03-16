package blocks.love

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import okhttp3.ResponseBody
import java.io.*


class MainActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    lateinit var recyclerAdapter: RecyclerAdapter

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

    public fun writeFile(body: ResponseBody, name: String) {
        try {
            // todo change the file location/name according to your needs
            val filePath = File(getExternalFilesDir(null).toString() + File.separator + name)
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0
                inputStream = body.byteStream()
                outputStream = FileOutputStream(filePath)
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
            } catch (e: IOException) {
                //todo handle error
                Log.d("DOWNLOAD", "file download was fail")
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            //todo handle error
            Log.d("DOWNLOAD", "file download was fail")
        }
    }
}
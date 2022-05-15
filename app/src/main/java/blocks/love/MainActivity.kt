package blocks.love

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import blocks.love.utils.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.OkHttpClient
import java.io.File

class MainActivity : AppCompatActivity() {

    internal lateinit var recyclerView: RecyclerView
    internal lateinit var recyclerAdapter: RecyclerAdapter
    private lateinit var auth: FirebaseAuth
    internal lateinit var mainLayout: ConstraintLayout
    internal val fileDownloader by lazy { FileDownloader(OkHttpClient.Builder().build()) }
    internal lateinit var loadingSpinner: CircularProgressIndicator

    companion object {
        const val WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 10
        const val READ_EXTERNAL_STORAGE_PERMISSION_CODE = 20
        const val REQUEST_INSTALL_PACKAGES_PERMISSION_CODE = 30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        when (Firebase.auth.currentUser) {
            null -> goToAuthActivity()
            else -> {
                checkIfAccessedFromFCMNotification()

                setContentView(R.layout.main_screen)
                mainLayout = findViewById<View>(R.id.mainLayout) as ConstraintLayout
                recyclerView = findViewById(R.id.recyclerView)
                recyclerAdapter = RecyclerAdapter(this)
                loadingSpinner = findViewById<View>(R.id.loading) as CircularProgressIndicator

                recyclerView.adapter = recyclerAdapter

                if(!isOnline()){
                    mainLayout.showDialog(R.string.connect_to_internet, R.string.connect_to_internet_title, this)
                }

                if (!checkGooglePlayServices()) {
                    mainLayout.showDialog(R.string.play_services, R.string.play_services_title, this)
                    Log.w("PLAY", "Device doesn't have google play services")
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    if (!writeFilePermissions()) {
                        Log.w("PLAY", "Device doesn't have permissions")
                    }
                }

                if (!isPackageInstalled("org.love2d.android")){
                    mainLayout.showDialogInstall(R.string.install_love, R.string.install_love_title, this, "org.love2d.android")
                    Log.w("PLAY", "Device doesn't have Love for Android installed")
                }

                val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (activityManager.isBackgroundRestricted){
                        mainLayout.showDialogInstall(R.string.background, R.string.background_title, this, "org.love2d.android")
                        Log.w("PLAY", "Device can't work in background")
                    }
                }

                getUserProjects()

                RxJavaPlugins.setErrorHandler { e ->
                    Log.e("Error", e.message.toString())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        when (Firebase.auth.currentUser) {
            null -> goToAuthActivity()
            else -> {
                val topAppBar = findViewById<View>(R.id.topAppBar) as MaterialToolbar
                topAppBar.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_projects -> { getUserProjects(); true }
                        R.id.menu_sign_out -> { signOutButton(); true }
                        R.id.menu_info -> { infoButton(); true }
                        else -> false
                    }
                }
            }
        }
    }

    /**
     * Check if app was opened from FCM notification
     */
    private fun checkIfAccessedFromFCMNotification(){
        val url = intent.getStringExtra("url")
        val name = intent.getStringExtra("name")
        if (url != null && name != null) {
            intent.removeExtra("name")
            intent.removeExtra("url")
            val fileName = name.replace(" ", "_")
            val loveFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + fileName + ".love"
            downloadLoveProject(
                url.replace("localhost", "192.168.0.20"),
                applicationContext,
                fileDownloader,
                loveFilePath,
//                recyclerAdapter.projectViewHolder
            )
        }
    }

    /**
     * SignOut Button onClick
     */
    private fun signOutButton() {
        Firebase.auth.signOut()
        goToAuthActivity()
    }

    /**
     * Info Button onCLick
     */
    private fun infoButton(){
        mainLayout.showDialog(R.string.how_it_works, R.string.how_it_works_title, this)
    }

    /**
     * Create Intent and go to Login Activity
     */
    private fun goToAuthActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    /**
     * Get all projects of current user
     */
    private fun getUserProjects(){
        val user = Firebase.auth.currentUser
        user?.getIdToken(true)?.addOnCompleteListener { getIDToken ->
            loadingSpinner.show()
            if (getIDToken.isSuccessful) {
                val idToken = getIDToken.result.token
                val projectData = ProjectsData( id_token = idToken!! )

                RestApiManager().getProjects(projectData) { responseData ->
                    if (responseData != null ) {
                        Log.d("PROJECTS", "OK")
                        recyclerAdapter.setProjectListItems(responseData)
                        loadingSpinner.hide()
                    } else {
                        mainLayout.showDialog(R.string.connect_to_server, R.string.connect_to_internet_title, this)
                        Log.d("PROJECTS", "NULL")
                        loadingSpinner.hide()
                    }
                }
            } else {
                //todo
                Log.d("PROJECTS", getIDToken.exception.toString())
                loadingSpinner.hide()
            }
        }
    }

    /**
     * Chek if google play services are installed
     *
     * @return
     */
    private fun checkGooglePlayServices(): Boolean {
        return if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            Log.e("PLAY", "Error update play services")
            mainLayout.showSnackBar(R.string.update_play_service, Snackbar.LENGTH_SHORT)
            false
        } else true
    }

    /**
     * Check if package is installed
     *
     * @param packageName
     * @return
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        val pm = packageManager
        try {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: NameNotFoundException) {
            Log.w("PLAY", "Not installed: $e")
            return false
        }
        return true
    }

    /**
     * Check if has access to internet
     *
     * @return
     */
    private fun isOnline(): Boolean {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
            }
        }
        return false
    }

    /**
     * Get write permission
     *
     * @return
     */
    private fun writeFilePermissions(): Boolean {
        requestPermission(WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE, R.string.storage_access_required)
        Log.d("DOWNLOAD", "Permission granted")
        return ActivityCompat.checkSelfPermission(this@MainActivity, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED
    }

    /**
     * Request permission
     *
     * @param permission
     * @param requestCode
     * @param message
     */
    private fun requestPermission(permission: String, requestCode: Int, message: Int) {
        if (ActivityCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, permission)) {
                mainLayout.showSnackBar(message, Snackbar.LENGTH_INDEFINITE, R.string.ok) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
                }
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            }
        }
    }

    /**
     * Result of permission request
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Write storage Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    mainLayout.showDialog(R.string.read_access_required, R.string.storage_permission_denied, this)
//                    mainLayout.showSnackBar(R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
                }
            }
            READ_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "Read storage Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Read storage Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
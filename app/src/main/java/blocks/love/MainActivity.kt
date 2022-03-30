package blocks.love

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import blocks.love.utils.FileDownloader
import blocks.love.utils.showDialog
import blocks.love.utils.showDialogInstall
import blocks.love.utils.showSnackBar
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: RecyclerAdapter
    private lateinit var auth: FirebaseAuth
    internal lateinit var mainLayout: ConstraintLayout
    internal val fileDownloader by lazy { FileDownloader(OkHttpClient.Builder().build()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        when (Firebase.auth.currentUser) {
            null -> goToAuthActivity()
            else -> {
                setContentView(R.layout.main_screen)

                mainLayout = findViewById<View>(R.id.mainLayout) as ConstraintLayout
                recyclerView = findViewById(R.id.recyclerView)
                recyclerAdapter = RecyclerAdapter(this)
                recyclerView.adapter = recyclerAdapter

                if(!isOnline()){
                    mainLayout.showDialog(R.string.connect_to_internet, R.string.connect_to_internet_title, this)
                }
                if (!checkGooglePlayServices()) {
                    mainLayout.showDialog(R.string.play_services, R.string.play_services_title, this)
                    Log.w("PLAY", "Device doesn't have google play services")
                }
                // https://developer.android.com/training/package-visibility
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    if (!isPackageInstalled("org.love2d.android")){
                        mainLayout.showDialogInstall(R.string.install_love, R.string.install_love_title, this, "org.love2d.android")
                        Log.w("PLAY", "Device doesn't have Love for Android installed")
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
                        else -> false
                    }
                }
            }
        }
    }

    // Signs Out user -> On button click
    private fun signOutButton() {
        Firebase.auth.signOut()
        goToAuthActivity()
//        AuthUI.getInstance().signOut(this).addOnCompleteListener { goToAuthActivity() }
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
                        mainLayout.showDialog(R.string.connect_to_server, R.string.connect_to_internet_title, this)
                        Log.d("PROJECTS", "NULL")
                    }
                }
            } else {
                //todo
                Log.d("PROJECTS", getIDToken.exception.toString())
            }
        }
    }

    private fun checkGooglePlayServices(): Boolean {
        return if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            Log.e("PLAY", "Error update play services")
            mainLayout.showSnackBar(R.string.update_play_service, Snackbar.LENGTH_SHORT)
            false
        } else true
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            this.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: NameNotFoundException) {
            Log.w("PLAY", "Not installed: $e")
            false
        }
    }

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
}
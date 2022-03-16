package blocks.love

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging


const val sharedPrefFile = "login_shared_preferences"
const val logged = "logged"
const val fcm_token = "fcm_token"

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        val user = Firebase.auth.currentUser
        setContentView(R.layout.activity_login)

        when {
            user != null -> userIsLoggedIn()
            else -> {
                val token = intent.getStringExtra("EXTRA_USER_TOKEN")
                when {
                    token != null -> loginWithCustomToken(token)
                    else -> userIsLoggedOut()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = Firebase.auth.currentUser

        when {
            user != null -> userIsLoggedIn()
            else -> userIsLoggedOut()
        }
    }

    // Runs when user is Signed In
//    private fun onUserSignedIn(user: FirebaseUser) {
//        setContentView(R.layout.main_screen)
//        val view_label = findViewById<View>(R.id.view_label) as TextView
//        val view_btn = findViewById<View>(R.id.view_btn) as Button
//        view_label.text = user.displayName
//        view_btn.setText("Sign Out")
//        view_btn.setOnClickListener { signOutButton() }

//        GET ID Token for backend calls
//        user.getIdToken(true).addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val idToken = task.result.token
//                // Send token to your backend via HTTPS
//                // ...
//            } else {
//                // Handle error -> task.getException();
//            }
//        }
//    }

    // Runs when user is Logged In
    private fun userIsLoggedIn() {
        sendRegistrationToServer()
        val sharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE)
        if (sharedPreferences.getBoolean(logged, false)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // Runs when user is Logged Out
    private fun userIsLoggedOut() {
        val loginEmail = findViewById<View>(R.id.login_email_edit) as EditText
        val loginPassword = findViewById<View>(R.id.login_password_edit) as EditText
        val buttonLogin = findViewById<View>(R.id.btn_login) as Button

        loginEmail.text = null
        loginPassword.text = null

        buttonLogin.setOnClickListener { loginButton() }
    }

    // Login user -> On button click
    private fun loginButton() {
        val loginEmail = findViewById<View>(R.id.login_email_edit) as EditText
        val loginPassword = findViewById<View>(R.id.login_password_edit) as EditText

        login(
            loginEmail.text.toString(),
            loginPassword.text.toString()
        )
    }

    // Activity result
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser!!
            userIsLoggedIn()
//            onUserSignedIn(user)
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
//            val response = IdpResponse.fromResultIntent(data)
            response?.error?.printStackTrace()
            userIsLoggedOut()
        }
    }

    fun registerOnClick(view: View?) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun login(email: String, password: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { getFCMToken ->
            if (getFCMToken.isSuccessful) {
                val fcmToken = getFCMToken.result
                val userData = LoginData(
                    email = email,
                    password = password,
                    fcm_token = fcmToken
                )
                Log.d("LOGIN", "Email: $email | Password: $password")
                Log.d("LOGIN", "FCM Token: $fcmToken")

                RestApiManager().loginUser(userData) { responseData ->
                    when {
                        responseData?.access_token != null -> {
                            Log.d("LOGIN", "WIN")
                            Log.d("LOGIN", responseData.id)
                            Toast.makeText(baseContext, "LOGIN WIN", Toast.LENGTH_SHORT).show()

                            loginWithCustomToken(responseData.access_token)
                        }
                        //todo Error
                        responseData?.errors?.error != null -> {
                            Log.d("LOGIN", responseData.errors.error)
                            Toast.makeText(baseContext, "LOGIN FAIL", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Log.d("LOGIN", "NULL")
                            Toast.makeText(baseContext, "LOGIN NULL", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // todo Handle error
                Log.w("TOKEN", "Fetching FCM registration token failed", getFCMToken.exception)
            }
        }
    }

    private fun loginWithCustomToken(firebaseToken: String){
        val sharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(logged, true).apply()

        firebaseToken.let { token ->
            auth.signInWithCustomToken(token).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LOGIN", "signInWithCustomToken:success")
                    userIsLoggedIn()
                } else {
                    Log.w("LOGIN", "signInWithCustomToken:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    userIsLoggedOut()
                }
            }
        }
    }

    /**
     * Gets FCM token, then user and ID Token -> sends
     *
     */
    private fun sendRegistrationToServer() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { getFCMToken ->
            if (getFCMToken.isSuccessful) {
                val fcmToken = getFCMToken.result
                val user = Firebase.auth.currentUser
                user?.getIdToken(true)?.addOnCompleteListener { getIDToken ->
                    if (getIDToken.isSuccessful) {
                        val idToken = getIDToken.result.token
                        val tokenData = TokenData(
                            id_token = idToken!!,
                            fcm_token = fcmToken,
                        )
                        Log.d("TOKEN", "Sending ID Token: $idToken")
                        RestApiManager().sendToken(tokenData) { responseData ->
                            when {
                                //todo Error
                                responseData?.errors?.error != null -> {
                                    Log.d("TOKEN", "response: " + responseData.errors.error)
                                }
                            }
                        }
                    } else {
                        // todo Handle error -> task.getException();
                        Log.w("TOKEN", "Fetching ID token failed", getIDToken.exception)
                    }
                }
                Log.d("TOKEN", "Sending FCM Token: $fcmToken")
            } else {
                // todo Handle error
                Log.w("TOKEN", "Fetching FCM registration token failed", getFCMToken.exception)
            }
        }
    }
}
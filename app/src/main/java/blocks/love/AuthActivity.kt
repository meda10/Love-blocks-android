package blocks.love

import android.os.Bundle
import android.security.ConfirmationAlreadyPresentingException
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var customToken: String? = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE2NDY4NDI2NzkuMTQzODc5LCJpc3MiOiJmaXJlYmFzZS1hZG1pbnNkay12OWExd0Bsb3ZlLWJsb2Nrcy5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbSIsImV4cCI6MTY0Njg0NjI3OS4xNDM4NzksInN1YiI6ImZpcmViYXNlLWFkbWluc2RrLXY5YTF3QGxvdmUtYmxvY2tzLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwiYXVkIjoiaHR0cHM6Ly9pZGVudGl0eXRvb2xraXQuZ29vZ2xlYXBpcy5jb20vZ29vZ2xlLmlkZW50aXR5LmlkZW50aXR5dG9vbGtpdC52MS5JZGVudGl0eVRvb2xraXQiLCJ1aWQiOiIxIn0.B8UUTOk1z47zWSbpIzoLeAnbBD9_Hw-xJyktL-GzqXmrvBTWeOQucVpyheUcN2_rATbRTdUnurw9NoUtEeP2h9g02d_dIP47ZA5T-LhxI-kBf7tEnaZL7eTgf470NTM1qtcJWqPmGFaeQQ4y6Y9YA013H-JIn6cFrk_pXRndxy0lPe6Ba1-nJf8gHL4vP3El8EnZvQK6jKKwqPjbbgd6UubXENqWdgwMuDSj0XswQqr1fgyL0C7UWUG6SIp-2_VdU1TfRe_00cU2WLXfJfEUMwda0Nx-crTjmiU1SJgDdOwbKaZTAHAP9Ck6Dnp2hFV4lseoRtusvN6yYg6aY8eMVw"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        setContentView(R.layout.activity_login)
    }

    override fun onStart() {
        super.onStart()
        val user = Firebase.auth.currentUser
        if (user != null) {
            onUserSignedIn(user)
        } else {
            onUserSignedOut()
        }
    }

    // Runs when user is Signed In
    private fun onUserSignedIn(user: FirebaseUser) {
        setContentView(R.layout.main_screen)
        val view_label = findViewById<View>(R.id.view_label) as TextView
        val view_btn = findViewById<View>(R.id.view_btn) as Button
        view_label.text = user.displayName
        view_btn.setText("Sign Out")
        view_btn.setOnClickListener { signOutButton() }

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
    }

    // Signs Out user -> On button click
    private fun signOutButton() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener { onUserSignedOut() }
    }

    // Runs when user is Signed Out
    private fun onUserSignedOut() {
        val loginEmail = findViewById<View>(R.id.login_email_edit) as EditText
        val loginPassword = findViewById<View>(R.id.login_password_edit) as EditText
        val buttonLogin = findViewById<View>(R.id.btn_login) as Button
        val buttonRegister = findViewById<View>(R.id.btn_register) as Button

        loginEmail.text = null
        loginPassword.text = null

        buttonLogin.setOnClickListener { signInButton() }
        buttonRegister.setOnClickListener { registerButton() }
    }

    // Signs In user -> On button click
    private fun signInButton() {
        val loginEmail = findViewById<View>(R.id.login_email_edit) as EditText
        val loginPassword = findViewById<View>(R.id.login_password_edit) as EditText

        val user = login(
            loginEmail.text.toString(),
            loginPassword.text.toString()
        )

//        customToken?.let {
//            auth.signInWithCustomToken(it)
//                .addOnCompleteListener(this) { task ->
//                    if (task.isSuccessful) {
//                        // Sign in success, update UI with the signed-in user's information
//                        Log.d("CUSTOM", "signInWithCustomToken:success")
//                        val user = auth.currentUser!!
//                        onUserSignedIn(user)
//                    } else {
//                        // If sign in fails, display a message to the user.
//                        Log.w("CUSTOM", "signInWithCustomToken:failure", task.exception)
//                        Toast.makeText(baseContext, "Authentication failed.",
//                            Toast.LENGTH_SHORT).show()
//                        onUserSignedOut()
//                    }
//                }
//        }
    }

    private fun registerButton() {
        val registerEmail = findViewById<View>(R.id.register_email_edit) as EditText
        val registerPassword = findViewById<View>(R.id.register_password_edit) as EditText
        val registerPasswordConfirm = findViewById<View>(R.id.register_password_confirm_edit) as EditText

        val user = register(
            registerEmail.text.toString(),
            registerPassword.text.toString(),
            registerPasswordConfirm.text.toString()
        )
    }

    // Activity result
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser!!
            onUserSignedIn(user)
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
//            val response = IdpResponse.fromResultIntent(data)
            response?.error?.printStackTrace()
            onUserSignedOut()
        }
    }

    fun loginOnClick(view: View?) {
        setContentView(R.layout.activity_login)
    }

    fun registerOnClick(view: View) {
        setContentView(R.layout.activity_register)
    }

    private fun login(email: String, password: String) {
        val apiManager = RestApiManager()
        val userData = LoginData( email = email, password = password )

        val a = apiManager.loginUser(userData) {
            when {
                it?.id != null -> {
                    Log.d("LOGIN", "WIN")
                    Log.d("LOGIN", it.id.toString())
                    Toast.makeText(baseContext, "LOGIN WIN", Toast.LENGTH_SHORT).show()
//                    return@loginUser "wad"
                }
                it?.error != null -> {
                    Log.d("LOGIN", it.error)
                    Toast.makeText(baseContext, "LOGIN FAIL", Toast.LENGTH_SHORT).show()
//                    return@loginUser "wad"
                }
                else -> {
                    Log.d("LOGIN", "NULL")
                    Log.d("LOGIN", it.toString())
                    Toast.makeText(baseContext, "LOGIN NULL", Toast.LENGTH_SHORT).show()
//                    return@loginUser "wad"
                }
            }
        }

        return a
    }

    private fun register(email: String, password: String, passwordConfirmation: String) {
        val apiManager = RestApiManager()
        val userData = RegisterData(
            name = "",
            email = email,
            password = password,
            password_confirmation = passwordConfirmation,
            terms = "accepted",
        )

        apiManager.registerUser(userData) {
            when {
                it?.id != null -> {
                    Log.d("REG", "WIN")
                    Log.d("REG", it.id.toString())
                    Toast.makeText(baseContext, "REG WIN", Toast.LENGTH_SHORT).show()
                }
                it?.error != null -> {
                    Log.d("REG", it.error)
                    Toast.makeText(baseContext, "REG FAIL", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.d("REG", "NULL")
                    Log.d("REG", it.toString())
                    Toast.makeText(baseContext, "REG NULL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}


//package blocks.love
//
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import blocks.love.MainActivity
//import com.firebase.ui.auth.AuthUI
//import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
//import java.util.*
//
//class LoginActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.login)
//
//        // find view
//        val buttonSignIn = findViewById<Button>(R.id.buttonSignIn)
//        // Set onclick listener
//        buttonSignIn.setOnClickListener {
//            val auth = FirebaseAuth.getInstance()
//            if (auth.currentUser != null) {
//                Toast.makeText(
//                    applicationContext, "User already signed in, must sign out first",
//                    Toast.LENGTH_SHORT
//                ).show()
//                // already signed in
//            } else {
//                // Choose authentication providers
//                val providers = Arrays.asList(
//                    EmailBuilder().build()
//                )
//
//                // Create and launch sign-in intent
//                startActivityForResult(
//                    AuthUI.getInstance()
//                        .createSignInIntentBuilder()
//                        .setAvailableProviders(providers)
//                        .build(),
//                    RC_SIGN_IN
//                )
//            }
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        // RC_SIGN_IN is the request code you passed into
//        if (requestCode == RC_SIGN_IN) {
//
//            // Successfully signed in
//            if (resultCode == RESULT_OK) {
//                // Successfully signed in
//                val user = FirebaseAuth.getInstance().currentUser
//                Toast.makeText(applicationContext, "Successfully signed in", Toast.LENGTH_SHORT)
//                    .show()
//                launchMainActivity(user)
//            }
//        } else {
//            // Sign in failed. If response is null the user canceled the sign-in flow using the back button. Otherwise check
//            // response.getError().getErrorCode() and handle the error.
//            Toast.makeText(applicationContext, "Unable to Sign in", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun launchMainActivity(user: FirebaseUser?) {
//        if (user != null) {
//            MainActivity.startActivity(this, user.displayName)
//            finish()
//        }
//    }
//
//    companion object {
//        private const val RC_SIGN_IN = 1234
//        fun startActivity(context: Context) {
//            val intent = Intent(context, LoginActivity::class.java)
//            context.startActivity(intent)
//        }
//    }
//}
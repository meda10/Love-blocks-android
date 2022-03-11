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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val sharedPrefFile = "login_shared_preferences"
    private val logged = "logged"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        setContentView(R.layout.activity_login)

        val user = Firebase.auth.currentUser
        if (user != null) {
            userIsLoggedIn()
        } else {
            userIsLoggedOut()
        }
    }

    override fun onStart() {
        super.onStart()
        val user = Firebase.auth.currentUser
        if (user != null) {
            userIsLoggedIn()
        } else {
            userIsLoggedOut()
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
        val sharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE)
        if (sharedPreferences.getBoolean(logged, false)) {
            val intent = Intent(this, MainActivity::class.java)
//        intent.putExtra("user", user)
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

    // Runs when user is not Registered
    private fun userIsNotRegistered() {
        val registerEmail = findViewById<View>(R.id.register_email_edit) as EditText
        val registerPassword = findViewById<View>(R.id.register_password_edit) as EditText
        val registerPasswordConfirm = findViewById<View>(R.id.register_password_confirm_edit) as EditText
        val buttonRegister = findViewById<View>(R.id.btn_register) as Button

        registerEmail.text = null
        registerPassword.text = null
        registerPasswordConfirm.text = null

        buttonRegister.setOnClickListener { registerButton() }
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

    private fun registerButton() {
        val registerEmail = findViewById<View>(R.id.register_email_edit) as EditText
        val registerPassword = findViewById<View>(R.id.register_password_edit) as EditText
        val registerPasswordConfirm = findViewById<View>(R.id.register_password_confirm_edit) as EditText

        register(
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

    fun loginOnClick(view: View?) {
        setContentView(R.layout.activity_login)
        userIsLoggedOut()
    }

    fun registerOnClick(view: View?) {
        setContentView(R.layout.activity_register)
        userIsNotRegistered()
    }

    private fun login(email: String, password: String) {
        val apiManager = RestApiManager()
        val userData = LoginData( email = email, password = password )
        Log.d("LOGIN", "Email: $email | Password: $password")

        apiManager.loginUser(userData) { responseData ->
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
                    Log.d("LOGIN", responseData.toString())
                    Toast.makeText(baseContext, "LOGIN NULL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun register(email: String, password: String, passwordConfirmation: String) {
        val apiManager = RestApiManager()
        val userData = RegisterData(
            name = "",
            email = "me@dd.czcx",
            password = "",
            password_confirmation = "password",
            terms = "accepted",
        )
        Log.d("REG", "Email: $email | Password: $password | Confirm: $passwordConfirmation")


        apiManager.registerUser(userData) { responseData ->
            when {
                responseData?.access_token != null -> {
                    Log.d("REG", "WIN")
                    Log.d("REG", responseData.id)
                    Toast.makeText(baseContext, "REG WIN", Toast.LENGTH_SHORT).show()
                }
                //todo Error
                responseData?.errors?.error != null -> {
                    Log.d("REG", responseData.errors.error)
                }
                responseData?.errors?.email != null -> {
                    for (i in 0 until responseData.errors.email.count()) Log.d("REG", responseData.errors.email[i])
                }
                responseData?.errors?.name != null -> {
                    for (i in 0 until responseData.errors.name.count()) Log.d("REG", responseData.errors.name[i])
                }
                responseData?.errors?.password != null -> {
                    for (i in 0 until responseData.errors.password.count()) Log.d("REG", responseData.errors.password[i])
                }
                else -> {
                    Log.d("REG", "NULL")
                    Log.d("REG", responseData.toString())
                    Toast.makeText(baseContext, "REG NULL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loginWithCustomToken(firebaseToken: String){
        val sharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(logged, true).apply()

        firebaseToken.let { token ->
            auth.signInWithCustomToken(token).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("CUSTOM", "signInWithCustomToken:success")
                    userIsLoggedIn()
                } else {
                    Log.w("CUSTOM", "signInWithCustomToken:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    userIsLoggedOut()
                }
            }
        }
    }
}
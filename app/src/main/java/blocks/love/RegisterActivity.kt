package blocks.love

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import blocks.love.utils.RegisterData
import blocks.love.utils.RestApiManager
import blocks.love.utils.showDialog
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authLayout: ScrollView
    private lateinit var loadingSpinner: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = Firebase.auth
        authLayout = findViewById<View>(R.id.registerLayout) as ScrollView
        loadingSpinner = findViewById<View>(R.id.loadingRegister) as CircularProgressIndicator
    }

    override fun onStart() {
        super.onStart()
        userIsNotRegistered()
    }


    /**
     * Runs when user is not Registered
     */
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

    /**
     * Register button onClick
     */
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

    /**
     * Send data to server and register user
     *
     * @param email
     * @param password
     * @param passwordConfirmation
     */
    private fun register(email: String, password: String, passwordConfirmation: String) {
        loadingSpinner.show()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { getFCMToken ->
            if (getFCMToken.isSuccessful) {
                val fcmToken = getFCMToken.result
                val userData = RegisterData(
                    name = email,
                    email = email,
                    password = password,
                    password_confirmation = passwordConfirmation,
                    terms = "accepted",
                    fcm_token = fcmToken
                )
                Log.d("REG", "Email: $email | Password: $password | Confirm: $passwordConfirmation")

                RestApiManager().registerUser(userData) { responseData ->
                    when {
                        responseData?.access_token != null -> {
                            Log.d("REG", "WIN")
                            Log.d("REG", responseData.id)
                            loginWithCustomToken(responseData.access_token)
                        }
                        responseData?.errors?.error != null -> {
                            Log.d("REG", responseData.errors.error)
                            loadingSpinner.hide()
                            authLayout.showDialog(responseData.errors.error, R.string.something_wrong_title, this)
                        }
                        responseData?.errors?.email != null -> {
                            for (i in 0 until responseData.errors.email.count()) Log.d("REG", responseData.errors.email[i])
                            loadingSpinner.hide()
                            authLayout.showDialog(responseData.errors.email[0], R.string.something_wrong_title, this)
                        }
                        responseData?.errors?.name != null -> {
                            for (i in 0 until responseData.errors.name.count()) Log.d("REG", responseData.errors.name[i])
                            loadingSpinner.hide()
                            authLayout.showDialog(responseData.errors.name[0], R.string.something_wrong_title, this)
                        }
                        responseData?.errors?.password != null -> {
                            for (i in 0 until responseData.errors.password.count()) Log.d("REG", responseData.errors.password[i])
                            loadingSpinner.hide()
                            authLayout.showDialog(responseData.errors.password[0], R.string.something_wrong_title, this)
                        }
                        else -> {
                            Log.d("REG", "NULL")
                            loadingSpinner.hide()
                            authLayout.showDialog(R.string.connect_to_server, R.string.something_wrong_title, this)
                        }
                    }
                }
            } else {
                // todo Handle FCM error
                loadingSpinner.hide()
                Log.w("TOKEN", "Fetching FCM registration token failed", getFCMToken.exception)
            }
        }
    }


    /**
     * Login Button onClick
     *
     * @param view
     */
    fun loginOnClick(view: View?) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    /**
     * Firebase login with custom token
     *
     * @param firebaseToken
     */
    private fun loginWithCustomToken(firebaseToken: String){
        val sharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(logged, true).apply()

        firebaseToken.let { token ->
            auth.signInWithCustomToken(token).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    loadingSpinner.hide()
                    Log.d("REG", "signInWithCustomToken:success")
                    userIsLoggedIn()
                } else {
                    loadingSpinner.hide()
                    Log.w("REG", "signInWithCustomToken:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    userIsNotRegistered()
                }
            }
        }
    }

    /**
     * Runs when user is Logged In
     */
    private fun userIsLoggedIn() {
        val sharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE)
        if (sharedPreferences.getBoolean(logged, false)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}
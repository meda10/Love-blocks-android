package blocks.love

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()
        userIsNotRegistered()
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

    private fun register(email: String, password: String, passwordConfirmation: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { getFCMToken ->
            if (getFCMToken.isSuccessful) {
                val fcmToken = getFCMToken.result
                val userData = RegisterData(
                    name = "Barry",
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
                            Toast.makeText(baseContext, "REG WIN", Toast.LENGTH_SHORT).show()

                            loginWithCustomToken(responseData.access_token)
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
            } else {
                // todo Handle error
                Log.w("TOKEN", "Fetching FCM registration token failed", getFCMToken.exception)
            }
        }
    }


    fun loginOnClick(view: View?) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun loginWithCustomToken(firebaseToken: String){
        val sharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(logged, true).apply()

        firebaseToken.let { token ->
            auth.signInWithCustomToken(token).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("REG", "signInWithCustomToken:success")
                    userIsLoggedIn()
                } else {
                    Log.w("REG", "signInWithCustomToken:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    userIsNotRegistered()
                }
            }
        }
    }

    // Runs when user is Logged In
    private fun userIsLoggedIn() {
        val sharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE)
        if (sharedPreferences.getBoolean(logged, false)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
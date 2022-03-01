package blocks.love

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.firebase.ui.auth.util.ExtraConstants
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    // Activity Callback
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res -> this.onSignInResult(res) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)
    }

    override fun onStart() {
        super.onStart()

        FirebaseAuth.getInstance().currentUser?.let {
            onUserSignedIn(it)
        } ?: onUserSignedOut()
    }

    // Runs when user is Signed In
    private fun onUserSignedIn(user: FirebaseUser) {
        val view_label = findViewById<View>(R.id.view_label) as TextView
        val view_btn = findViewById<View>(R.id.view_btn) as Button
        view_label.text = user.displayName
        view_btn.setText("Sign Out")
        view_btn.setOnClickListener { signOut() }
    }

    // Signs Out user -> On button click
    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener { onUserSignedOut() }
    }

    // Runs when user is Signed Out
    private fun onUserSignedOut() {
        val view_label = findViewById<View>(R.id.view_label) as TextView
        val view_btn = findViewById<View>(R.id.view_btn) as Button
        view_label.text = null
        view_btn.setText("Sign In")
        view_btn.setOnClickListener { signIn() }
    }

    // Signs In user -> On button click
    private fun signIn() {
        val idpConfigs = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(idpConfigs)
            .build()

        signInLauncher.launch(signInIntent)
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
}
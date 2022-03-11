package blocks.love

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)
    }

    override fun onStart() {
        super.onStart()

        val user = Firebase.auth.currentUser
        val view_label = findViewById<View>(R.id.view_label) as TextView
        val view_btn = findViewById<View>(R.id.view_btn) as Button
        if (user != null) {
            view_label.text = user.displayName
        } else {
            view_label.text = "null"
        }
        view_btn.text = "Sign Out"
        view_btn.setOnClickListener { signOutButton() }
    }

    // Signs Out user -> On button click
    private fun signOutButton() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener { goToAuthActivity() }
    }

    private fun goToAuthActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}
package com.example.easyscreen

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class UserActivity : ComponentActivity() {

    private lateinit var emailText: EditText
    private lateinit var passwordText : EditText
    private lateinit var vipTextView: TextView
    private lateinit var sharedPrefs: SharedPreferences
    private var isEditing: Boolean = false

    private lateinit var login_button: Button
    private lateinit var sign_up_button: Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var logoutButton: Button

    private var isInput = false
    private var isSignUp = false
    private var isLogin = false

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "GoogleSignIn"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        emailText = findViewById(R.id.name_edit_text)
        passwordText = findViewById(R.id.email_edit_text)
        vipTextView = findViewById(R.id.vip_text_view)
        sign_up_button = findViewById(R.id.sign_up_button)
        logoutButton = findViewById(R.id.logout_button)
        login_button = findViewById(R.id.login_button)

        // get SharedPreferences
        sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loadUserInfo()


//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(R.string.default_web_client_id.toString())
//            .requestEmail()
//            .build()
//
//        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()


        // login listener
        login_button.setOnClickListener {
            if (isInput) {
                val email = emailText.text.toString()
                val password = passwordText.text.toString()

                // Call signIn method with email and password
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    signInWithEmailPassword(email, password)
                    login_button.text = "Login"
                    isInput = false
                    emailText.isEnabled = false
                    passwordText.isEnabled = false
                    isLogin = true
                    logoutButton.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                }
            } else {
                isInput = true
                emailText.isEnabled = true
                passwordText.isEnabled = true
                login_button.text = "Confirm"
            }

        }

        // sign up listener
        sign_up_button.setOnClickListener {
            if (isSignUp) {
                val email = emailText.text.toString()
                val password = passwordText.text.toString()

                // Validate input
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    registerUser(email, password)
                    sign_up_button.text = "Sign Up"
                    isSignUp = false
                    emailText.isEnabled = false
                    passwordText.isEnabled = false
                } else {
                    Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                }
            } else {
                isSignUp = true
                emailText.isEnabled = true
                passwordText.isEnabled = true
                sign_up_button.text = "Confirm"
            }
//            val email = emailText.text.toString()
//            val password = passwordText.text.toString()
//
//            // Validate input
//            if (email.isNotEmpty() && password.isNotEmpty()) {
//                registerUser(email, password)
//            } else {
//                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
//            }
        }

        logoutButton.setOnClickListener {
            logoutUser()
            isLogin = false
            logoutButton.visibility = View.INVISIBLE
        }
    }
    private fun registerUser(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "Registration successful, Welcome ${user?.email}", Toast.LENGTH_SHORT).show()
                } else {
                    // Registration failed
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun logoutUser() {
        firebaseAuth.signOut()
        emailText.setText("email")
        passwordText.setText("......")
        Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

//    private fun signInWithGoogle() {
//        val signInIntent = googleSignInClient.signInIntent
//        startActivityForResult(signInIntent, RC_SIGN_IN)
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(Exception::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Google 登录失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "登录成功: ${user?.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Firebase 登录失败", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadUserInfo() {
        val name = sharedPrefs.getString("name", "email")
        val email = sharedPrefs.getString("email", "password")
        val vipStatus = sharedPrefs.getBoolean("vip", false)

        emailText.setText(name)
        passwordText.setText(email)
        vipTextView.text = if (vipStatus) "VIP user" else "normal user"

        disableEditing()
    }

    private fun enableEditing() {
        emailText.isEnabled = true
        passwordText.isEnabled = true
    }

    private fun disableEditing() {
        emailText.isEnabled = false
        passwordText.isEnabled = false
    }

    private fun saveUserInfo() {
        val name = emailText.text.toString()
        val email = passwordText.text.toString()
        val isVip = vipTextView.text == "normal user"

        with(sharedPrefs.edit()) {
            putString("name", name)
            putString("email", email)
            putBoolean("vip", isVip)
            apply()
        }

        Toast.makeText(this, "Info saved", Toast.LENGTH_SHORT).show()
    }


}

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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class UserActivity : ComponentActivity() {

    private lateinit var emailText: EditText
    private lateinit var passwordText : EditText
    private lateinit var vipTextView: TextView
//    private lateinit var editCompleteButton: Button  // 合并的按钮
//    private lateinit var logoutButton: Button  // 退出登录按钮
    private lateinit var sharedPrefs: SharedPreferences
    private var isEditing: Boolean = false  // 标记是否处于编辑模式

    private lateinit var login_button: Button  // 登录按钮
    private lateinit var sign_up_button: Button  // 退出登录按钮
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

        // 初始化视图
        emailText = findViewById(R.id.name_edit_text)
        passwordText = findViewById(R.id.email_edit_text)
        vipTextView = findViewById(R.id.vip_text_view)
//        editCompleteButton = findViewById(R.id.edit_complete_button)
//        logoutButton = findViewById(R.id.logout_button)  // 初始化退出登录按钮
        sign_up_button = findViewById(R.id.sign_up_button)  // 初始化注册按钮
        logoutButton = findViewById(R.id.logout_button)

        login_button = findViewById(R.id.login_button)  // 初始化登录按钮

        // 获取 SharedPreferences
        sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // 加载用户信息
        loadUserInfo()

        // 按钮点击事件
//        editCompleteButton.setOnClickListener {
//            if (isEditing) {
//                // 在编辑模式下，保存用户信息并退出编辑模式
//                saveUserInfo()
//                toggleEditing(false)  // 退出编辑模式
//            } else {
//                // 进入编辑模式
//                toggleEditing(true)  // 进入编辑模式
//            }
//        }

        // 退出登录按钮点击事件
//        logoutButton.setOnClickListener {
//            logout()  // 调用退出登录方法
//        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(R.string.default_web_client_id.toString()) // 替换为您的客户端 ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()


        // 设置 Google 登录按钮点击事件
//        login_button.setOnClickListener {
//            signInWithGoogle()
//        }
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
                    // Redirect to login screen or main activity
                    // startActivity(Intent(this, LoginActivity::class.java))
                } else {
                    // Registration failed
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun logoutUser() {
        // 执行注销操作
        firebaseAuth.signOut()
        emailText.setText("email")
        passwordText.setText("......")

        // 显示注销成功的提示
        Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()

        // 跳转到登录页面，或者其他适当的页面
        // startActivity(Intent(this, LoginActivity::class.java))
        // finish()  // 如果需要结束当前页面
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

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

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
                    // 跳转到主页面或其他逻辑
                } else {
                    Toast.makeText(this, "Firebase 登录失败", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 加载用户信息
    private fun loadUserInfo() {
        val name = sharedPrefs.getString("name", "email")
        val email = sharedPrefs.getString("email", "password")
        val vipStatus = sharedPrefs.getBoolean("vip", false)

        emailText.setText(name)
        passwordText.setText(email)
        vipTextView.text = if (vipStatus) "VIP user" else "normal user"

        // 加载完成后禁用编辑
        disableEditing()
    }

    // 切换编辑模式
//    private fun toggleEditing(editing: Boolean) {
//        isEditing = editing
//        if (isEditing) {
//            enableEditing()
//            editCompleteButton.text = "finish"  // 改变按钮文本为“完成”
//        } else {
//            disableEditing()
//            editCompleteButton.text = "edit"  // 改变按钮文本为“编辑”
//        }
//    }

    // 启用编辑模式
    private fun enableEditing() {
        emailText.isEnabled = true
        passwordText.isEnabled = true
    }

    // 禁用编辑模式
    private fun disableEditing() {
        emailText.isEnabled = false
        passwordText.isEnabled = false
    }

    // 保存用户信息
    private fun saveUserInfo() {
        val name = emailText.text.toString()
        val email = passwordText.text.toString()
        val isVip = vipTextView.text == "normal user"  // 根据显示的文本判断VIP状态

        with(sharedPrefs.edit()) {
            putString("name", name)
            putString("email", email)
            putBoolean("vip", isVip)
            apply()
        }

        // 提示用户信息已保存
        Toast.makeText(this, "Info saved", Toast.LENGTH_SHORT).show()
    }

    // 退出登录
    private fun logout() {
        with(sharedPrefs.edit()) {
            clear()  // 清除所有用户信息
            apply()
        }

        // 提示用户已退出登录
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

        // 更新界面，重新加载用户信息
        loadUserInfo()
    }
}

package com.example.easyscreen

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class UserActivity : ComponentActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var vipTextView: TextView
    private lateinit var editCompleteButton: Button  // 合并的按钮
    private lateinit var logoutButton: Button  // 退出登录按钮
    private lateinit var sharedPrefs: SharedPreferences
    private var isEditing: Boolean = false  // 标记是否处于编辑模式

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        // 初始化视图
        nameEditText = findViewById(R.id.name_edit_text)
        emailEditText = findViewById(R.id.email_edit_text)
        vipTextView = findViewById(R.id.vip_text_view)
        editCompleteButton = findViewById(R.id.edit_complete_button)
        logoutButton = findViewById(R.id.logout_button)  // 初始化退出登录按钮

        // 获取 SharedPreferences
        sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // 加载用户信息
        loadUserInfo()

        // 按钮点击事件
        editCompleteButton.setOnClickListener {
            if (isEditing) {
                // 在编辑模式下，保存用户信息并退出编辑模式
                saveUserInfo()
                toggleEditing(false)  // 退出编辑模式
            } else {
                // 进入编辑模式
                toggleEditing(true)  // 进入编辑模式
            }
        }

        // 退出登录按钮点击事件
        logoutButton.setOnClickListener {
            logout()  // 调用退出登录方法
        }
    }

    // 加载用户信息
    private fun loadUserInfo() {
        val name = sharedPrefs.getString("name", "null")
        val email = sharedPrefs.getString("email", "null")
        val vipStatus = sharedPrefs.getBoolean("vip", false)

        nameEditText.setText(name)
        emailEditText.setText(email)
        vipTextView.text = if (vipStatus) "VIP user" else "normal user"

        // 加载完成后禁用编辑
        disableEditing()
    }

    // 切换编辑模式
    private fun toggleEditing(editing: Boolean) {
        isEditing = editing
        if (isEditing) {
            enableEditing()
            editCompleteButton.text = "finish"  // 改变按钮文本为“完成”
        } else {
            disableEditing()
            editCompleteButton.text = "edit"  // 改变按钮文本为“编辑”
        }
    }

    // 启用编辑模式
    private fun enableEditing() {
        nameEditText.isEnabled = true
        emailEditText.isEnabled = true
    }

    // 禁用编辑模式
    private fun disableEditing() {
        nameEditText.isEnabled = false
        emailEditText.isEnabled = false
    }

    // 保存用户信息
    private fun saveUserInfo() {
        val name = nameEditText.text.toString()
        val email = emailEditText.text.toString()
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

package com.touchin.lockplay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AdminPolicyComplianceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val intent = intent
        setResult(RESULT_OK, intent)
        finish()
    }
}
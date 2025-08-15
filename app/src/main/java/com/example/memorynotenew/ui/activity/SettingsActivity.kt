package com.example.memorynotenew.ui.activity

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.ActivitySettingsBinding
import com.example.memorynotenew.ui.fragment.PasswordFragment
import com.example.memorynotenew.ui.fragment.SettingsFragment
import com.example.memorynotenew.utils.PasswordManager

class SettingsActivity : AppCompatActivity() {
    private val binding by lazy { ActivitySettingsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)

        // Fragment 전환 시 갱신
        supportFragmentManager.addOnBackStackChangedListener {
            setupActionBar()
        }
        // 최초 실행 시 Fragment 삽입
        if (savedInstanceState == null) {
            replaceFragment(SettingsFragment())
        }
        onBackPressedDispatcher.addCallback(this@SettingsActivity, true) {
            finish()
        }
    }

    private fun setupActionBar() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)

        val title = when (currentFragment) {
            is SettingsFragment -> getString(R.string.settings)
            is PasswordFragment -> {
                val storedPassword = PasswordManager.getPassword(this@SettingsActivity)
                if (storedPassword.isNullOrEmpty()) {
                    getString(R.string.password_save)
                } else {
                    getString(R.string.password_change)
                }
            }
            else -> ""
        }
        supportActionBar?.apply {
            this.title = title
            setDisplayHomeAsUpEnabled(currentFragment is SettingsFragment)
        }
    }

    // 업 버튼 동작
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
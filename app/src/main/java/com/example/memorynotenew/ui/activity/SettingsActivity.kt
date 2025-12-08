package com.example.memorynotenew.ui.activity

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.PURPOSE
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.databinding.ActivitySettingsBinding
import com.example.memorynotenew.ui.fragment.DeleteAccountFragment
import com.example.memorynotenew.ui.fragment.FindPwFragment
import com.example.memorynotenew.ui.fragment.SignInFragment
import com.example.memorynotenew.ui.fragment.PasswordFragment
import com.example.memorynotenew.ui.fragment.SettingsFragment
import com.example.memorynotenew.ui.fragment.SignUpFragment
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

        // 백 스택 변경을 감지할 리스너를 먼저 등록
        // 프래그먼트 전환 시 갱신
        supportFragmentManager.addOnBackStackChangedListener {
            setupActionBar()
        }
        // 최초 실행 시 SettingsFragment 삽입
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SettingsFragment())
                .commit() // 비동기 -> 프래그먼트가 즉시 붙지 않음
            // 프래그먼트가 attach된 후 안전하게 갱신
            binding.root.post {
                setupActionBar()
            }
        }
        // 뒤로가기 동작
        onBackPressedDispatcher.addCallback(this) {
            with(supportFragmentManager) {
                // 백 스택 있으면 -> 이전 화면으로 이동
                if (backStackEntryCount > 0) {
                    popBackStack()
                } else { // 백 스택 없으면 -> 액티비티 종료
                    finish()
                }
            }
        }
    }

    fun setupActionBar() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        val title = getFragmentTitle(currentFragment)

        val showUpButton = when (currentFragment) {
            is SettingsFragment,
            is SignInFragment,
            is SignUpFragment,
            is FindPwFragment,
            is DeleteAccountFragment -> true
            is PasswordFragment -> false
            else -> false
        }
        supportActionBar?.apply {
            this.title = title
            setDisplayHomeAsUpEnabled(showUpButton)
        }
    }

    private fun getFragmentTitle(currentFragment: Fragment?): String = when(currentFragment) {
        is SettingsFragment -> getString(R.string.settings)
        is PasswordFragment -> getPasswordFragmentTitle(currentFragment)
        is SignInFragment -> getString(R.string.sign_in)
        is SignUpFragment -> getString(R.string.sign_up)
        is FindPwFragment -> getString(R.string.change_lock_password)
        is DeleteAccountFragment -> getString(R.string.delete_account)
        else -> ""
    }

    private fun getPasswordFragmentTitle(currentFragment: PasswordFragment): String {
        val purposeString = currentFragment.arguments?.getString(PURPOSE)
        val purpose = purposeString?.let { PasswordPurpose.valueOf(it) }

        return when (purpose) {
            PasswordPurpose.BACKUP -> getString(R.string.backup_memo) // 메모 백업
            else -> {
                val storedPassword = PasswordManager.getPassword(this)
                if (storedPassword.isNullOrEmpty()) {
                    getString(R.string.create_lock_password) // 잠금 비밀번호 생성
                } else {
                    getString(R.string.change_lock_password) // 잠금 비밀번호 변경
                }
            }
        }
    }

    // 업 버튼 동작
    override fun onSupportNavigateUp(): Boolean {
        with(supportFragmentManager) {
            // 백 스택 있으면 -> 이전 화면으로 이동
            return if (backStackEntryCount > 0) {
                popBackStack()
                true
            } else { // 백 스택 없으면 -> 액티비티 종료
                finish()
                true
            }
        }
        return true
    }
}
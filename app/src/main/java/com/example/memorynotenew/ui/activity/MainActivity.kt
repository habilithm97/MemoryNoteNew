package com.example.memorynotenew.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.databinding.ActivityMainBinding
import com.example.memorynotenew.room.memo.Memo
import com.example.memorynotenew.ui.fragment.ListFragment
import com.example.memorynotenew.ui.fragment.MemoFragment
import com.example.memorynotenew.ui.fragment.PasswordFragment

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

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
            invalidateOptionsMenu()
        }
        // 최초 실행 시 Fragment 삽입
        if (savedInstanceState == null) {
            replaceFragment(ListFragment())
        }
    }

    private fun setupActionBar() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)

        val title = when (currentFragment) {
            is ListFragment -> getString(R.string.app_name)
            is MemoFragment -> getString(R.string.memo)
            is PasswordFragment -> {
                // PasswordPurpose 가져오기 (LOCK, OPEN)
                val purposeString = currentFragment.arguments?.getString("password_purpose")
                val purpose = if (purposeString != null) {
                    PasswordPurpose.valueOf(purposeString) // enum으로 변환
                } else {
                    null
                }
                // OPEN이면 title 없음
                if (purpose == PasswordPurpose.OPEN) {
                    ""
                } else { // LOCK이면 잠금 여부에 따라 title 설정
                    val memo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        currentFragment.arguments?.getParcelable(Constants.MEMO, Memo::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        currentFragment.arguments?.getParcelable(Constants.MEMO)
                    }
                    if (memo?.isLocked == true) {
                        getString(R.string.unlock_memo)
                    } else {
                        getString(R.string.lock_memo)
                    }
                }
            }
            else -> ""
        }
        supportActionBar?.apply {
            this.title = title
            setDisplayHomeAsUpEnabled(currentFragment is MemoFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        return if (currentFragment is ListFragment) {
            menuInflater.inflate(R.menu.menu_main, menu)
            return true
        } else {
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        if (currentFragment is ListFragment) {
            return when (item.itemId) {
                R.id.setting -> {
                    val intent = Intent(this, SettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(intent)
                    true
                }
                R.id.select -> {
                    toggleMenuVisibility(item.itemId)
                    currentFragment.setMultiSelect(true)
                    true
                }
                R.id.cancel -> {
                    toggleMenuVisibility(item.itemId)
                    currentFragment.setMultiSelect(false)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item) // 기본 처리 위임
    }

    // 업 버튼 동작
    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun toggleMenuVisibility(itemId: Int) {
        val isMultiSelect = itemId == R.id.select
        binding.toolbar.menu.apply {
            findItem(R.id.setting)?.isVisible = !isMultiSelect
            findItem(R.id.select)?.isVisible = !isMultiSelect
            findItem(R.id.cancel)?.isVisible = isMultiSelect
        }
    }
}
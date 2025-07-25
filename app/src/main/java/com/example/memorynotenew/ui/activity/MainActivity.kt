package com.example.memorynotenew.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.ActivityMainBinding
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

        // 프래그먼트 전환 시 갱신
        supportFragmentManager.addOnBackStackChangedListener {
            setupActionBar()
            invalidateOptionsMenu()
        }
        // 최초 실행 시 프래그먼트 삽입
        if (savedInstanceState == null) {
            replaceFragment(ListFragment())
            setupActionBar()
            invalidateOptionsMenu()
        }
    }

    private fun setupActionBar() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)

        val title = when (currentFragment) {
            is ListFragment -> getString(R.string.app_name)
            is MemoFragment -> getString(R.string.memo)
            is PasswordFragment -> getString(R.string.lock_memo)
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
        return when (item.itemId) {
            R.id.setting -> {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
}
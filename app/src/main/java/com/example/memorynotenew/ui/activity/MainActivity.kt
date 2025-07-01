package com.example.memorynotenew.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.ActivityMainBinding
import com.example.memorynotenew.ui.fragment.ListFragment

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
        initView()

        if (savedInstanceState == null) {
            replaceFragment(ListFragment())
        }
    }

    private fun initView() {
        binding.apply {
            setSupportActionBar(toolbar)
        }
    }

    // MemoFragment 업 버튼 활성화
    fun showUpButton(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

    // MemoFragment 업 버튼 클릭 시 동작
    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
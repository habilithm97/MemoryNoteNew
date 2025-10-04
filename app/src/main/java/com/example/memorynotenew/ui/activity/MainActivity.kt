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
import com.example.memorynotenew.common.Constants.MEMO
import com.example.memorynotenew.common.Constants.PURPOSE
import com.example.memorynotenew.common.PasswordPurpose
import com.example.memorynotenew.databinding.ActivityMainBinding
import com.example.memorynotenew.room.entity.Memo
import com.example.memorynotenew.ui.fragment.ListFragment
import com.example.memorynotenew.ui.fragment.MemoFragment
import com.example.memorynotenew.ui.fragment.PasswordFragment
import com.example.memorynotenew.ui.fragment.TrashFragment

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

        // 백스택 변경을 감지할 리스너를 먼저 등록
        // 프래그먼트 전환 시 갱신
        supportFragmentManager.addOnBackStackChangedListener {
            setupActionBar()
            invalidateOptionsMenu()
        }
        // 최초 실행 시 프래그먼트 삽입
        if (savedInstanceState == null) {
            replaceFragment(ListFragment())
        }
    }

    private fun setupActionBar() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        val title = getFragmentTitle(currentFragment)

        supportActionBar?.apply {
            this.title = title
            setDisplayHomeAsUpEnabled(
                when (currentFragment) {
                    is MemoFragment, is TrashFragment -> true
                    else -> false
                }
            )
        }
    }

    private fun getFragmentTitle(currentFragment: Fragment?): String = when (currentFragment) {
        is ListFragment -> getString(R.string.app_name)
        is MemoFragment -> getString(R.string.memo)
        is PasswordFragment -> getPasswordFragmentTitle(currentFragment)
        is TrashFragment -> getString(R.string.trash)
        else -> ""
    }

    private fun getPasswordFragmentTitle(currentFragment: PasswordFragment): String {
        val purposeString = currentFragment.arguments?.getString(PURPOSE)
        val purpose = purposeString?.let { PasswordPurpose.valueOf(it) }

        return when (purpose) {
            PasswordPurpose.LOCK -> {
                val memo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    currentFragment.arguments?.getParcelable(MEMO, Memo::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    currentFragment.arguments?.getParcelable(MEMO)
                }
                // 메모 잠금 여부에 따라 제목 설정
                if (memo?.isLocked == true) {
                    getString(R.string.unlock_memo)
                } else {
                    getString(R.string.lock_memo)
                }
            }
            PasswordPurpose.OPEN -> ""
            PasswordPurpose.DELETE -> getString(R.string.delete_memo)
            else -> ""
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)

        return when (currentFragment) {
            is ListFragment, is TrashFragment -> {
                menuInflater.inflate(R.menu.menu_main, menu)
                true
            } else -> false
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)

        if (currentFragment is TrashFragment && menu != null) {
            menu.findItem(R.id.select)?.apply {
                isVisible = true
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
            menu.findItem(R.id.empty)?.apply {
                isVisible = true
                setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
            listOf(R.id.setting, R.id.cancel, R.id.restore, R.id.delete, R.id.all, R.id.trash).forEach {
                menu.findItem(it)?.isVisible = false
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)

        return when (currentFragment) {
            is ListFragment -> {
                when (item.itemId) {
                    R.id.setting -> {
                        val intent = Intent(this, SettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                        true
                    }
                    R.id.select -> {
                        currentFragment.setMultiSelect(true)
                        toggleMenuVisibility(currentFragment,true)
                        true
                    }
                    R.id.trash -> {
                        replaceFragment(TrashFragment())
                        true
                    }
                    R.id.cancel -> {
                        currentFragment.setMultiSelect(false)
                        toggleMenuVisibility(currentFragment, false)
                        true
                    }
                    R.id.delete -> {
                        currentFragment.deleteSelectedMemos()
                        true
                    }
                    R.id.all -> {
                        currentFragment.toggleSelectAll()
                        true
                    }
                    else -> super.onOptionsItemSelected(item)
                }
            }
            is TrashFragment -> {
                when (item.itemId) {
                    R.id.select -> {
                        currentFragment.setMultiSelect(true)
                        toggleMenuVisibility(currentFragment, true)
                        true
                    }
                    R.id.empty -> {
                        currentFragment.showEmptyTrashDialog()
                        true
                    }
                    R.id.cancel -> {
                        currentFragment.setMultiSelect(false)
                        toggleMenuVisibility(currentFragment, false)
                        true
                    }
                    R.id.restore -> {
                        currentFragment.restoreSelectedMemos()
                        true
                    }
                    R.id.delete -> {
                        currentFragment.deleteSelectedMemos()
                        true
                    }
                    R.id.all -> {
                        currentFragment.toggleSelectAll()
                        true
                    }
                    else -> super.onOptionsItemSelected(item)
                }
            }
            else -> super.onOptionsItemSelected(item) // 기본 처리 위임
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

    fun toggleMenuVisibility(currentFragment: Fragment, isMultiSelect: Boolean) {
        binding.toolbar.menu?.apply {
            when (currentFragment) {
                is ListFragment -> {
                    findItem(R.id.setting)?.isVisible = !isMultiSelect
                    findItem(R.id.select)?.isVisible = !isMultiSelect
                    findItem(R.id.trash)?.isVisible = !isMultiSelect
                    findItem(R.id.cancel)?.isVisible = isMultiSelect
                    findItem(R.id.delete)?.isVisible = isMultiSelect
                    findItem(R.id.all)?.isVisible = isMultiSelect
                }
                is TrashFragment -> {
                    findItem(R.id.select)?.isVisible = !isMultiSelect
                    findItem(R.id.empty)?.isVisible = !isMultiSelect
                    findItem(R.id.cancel)?.isVisible = isMultiSelect
                    findItem(R.id.restore)?.isVisible = isMultiSelect
                    findItem(R.id.delete)?.isVisible = isMultiSelect
                    findItem(R.id.all)?.isVisible = isMultiSelect
                }
            }
        }
    }
}
package com.example.memorynotenew.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.memorynotenew.R
import com.example.memorynotenew.common.PopupAction
import com.example.memorynotenew.databinding.ItemMemoBinding
import com.example.memorynotenew.room.memo.Memo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 아이템 클릭 시 실행될 동작을 외부에서 전달 받음
class MemoAdapter(private val onItemClick: (Memo) -> Unit,
                  private val onItemLongClick: (Memo, PopupAction) -> Unit) :
    ListAdapter<Memo, MemoAdapter.MemoViewHolder>(DIFF_CALLBACK) {

        private var memos: List<Memo> = emptyList() // 원본 메모 리스트
    private var selectedMemos = mutableSetOf<Int>() // 선택한 메모 리스트

    var isMultiSelect = false
    // isMultiSelect 값이 바뀔 때마다 실행
    set(value) {
        field = value // isMultiSelect의 실제 값을 바꿔줌
        if (!value) selectedMemos.clear() // 메모 선택 모드 해제 시 체크 상태 초기화
        notifyDataSetChanged() // 전체 아이템 갱신
    }

    inner class MemoViewHolder(private val binding: ItemMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(memo: Memo) {
                with(binding) {
                    // Memo 데이터를 각 View에 할당
                    tvContent.text = memo.content
                    tvDate.text = SimpleDateFormat(itemView.context.getString(R.string.date_format),
                        Locale.getDefault()).format(Date(memo.date))
                    imageView.visibility = if (memo.isLocked) View.VISIBLE else View.INVISIBLE

                    // 유효한 아이템을 가리킬 때만 실행
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        checkBox.apply {
                            visibility = if (isMultiSelect) View.VISIBLE else View.GONE
                            isChecked = adapterPosition in selectedMemos // 선택한 메모면 체크

                            // 체크 상태 변경 시 selectedMemos에 추가/제거
                            setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    selectedMemos.add(adapterPosition)
                                } else {
                                    selectedMemos.remove(adapterPosition)
                                }
                            }
                        }
                    }
                    with(root) {
                        setOnClickListener {
                            onItemClick(memo)
                        }
                        setOnLongClickListener {
                            showPopupMenu(it, memo)
                            true // 클릭 이벤트 발생 방지 (롱클릭 이벤트만 소비)
                        }
                    }
                }
            }
        private fun showPopupMenu(view: View, memo: Memo) {
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.item_popup_menu, menu)

                val lockMenuItem = menu.findItem(R.id.lock)
                lockMenuItem.title = if (memo.isLocked) {
                    view.context.getString(R.string.unlock)
                } else {
                    view.context.getString(R.string.lock)
                }
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.delete -> {
                            onItemLongClick(memo, PopupAction.DELETE)
                            true
                        }
                        R.id.lock -> {
                            onItemLongClick(memo, PopupAction.LOCK)
                            true
                        }
                        else -> false
                    }
                }
                setForceShowIcon(true) // 아이콘 강제 표시
                show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoAdapter.MemoViewHolder {
        val binding = ItemMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemoAdapter.MemoViewHolder, position: Int) {
        // 현재 위치의 Memo 데이터를 ViewHolder에 바인딩
        holder.bind(getItem(position))
    }

    companion object { // 클래스 수준의 정적 객체 (static처럼 동작)
        // RecyclerView 성능 최적화를 위해 변경 사항만 업데이트
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Memo>() {
            override fun areItemsTheSame(oldItem: Memo, newItem: Memo): Boolean {
                return oldItem.id == newItem.id
            }
            override fun areContentsTheSame(oldItem: Memo, newItem: Memo): Boolean {
                return oldItem == newItem
            }
        }
    }

    fun submitMemos(memos: List<Memo>) {
        this.memos = memos // 원본 메모 리스트 보관
        submitList(memos)
    }

    fun filterList(searchQuery: String, onFilterComplete: () -> Unit) {
        val filteredList = if (searchQuery.isEmpty()) {
            memos
        } else { // 공백도 검색 가능
            memos.filter {
                it.content.contains(searchQuery, ignoreCase = true) // 대소문자 구분 없이 검색
            }
        }
        submitList(filteredList) {
            onFilterComplete() // 필터링 후속 작업
        }
    }

    fun toggleSelectAll() {
        // 선택된 메모 수 = 전체 메모 수 -> 전체 선택이면
        if (selectedMemos.size == currentList.size) {
            selectedMemos.clear() // 전체 선택 해제
        } else { // 전체 선택이 아니면
            // 전체 선택
            selectedMemos.apply {
                clear()
                addAll(currentList.indices)
            }
        }
        notifyDataSetChanged()
    }
}
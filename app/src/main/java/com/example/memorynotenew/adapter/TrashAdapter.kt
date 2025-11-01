package com.example.memorynotenew.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants.MAX_TRASH_DAYS
import com.example.memorynotenew.common.Constants.ONE_DAYS_MS
import com.example.memorynotenew.databinding.ItemMemoBinding
import com.example.memorynotenew.room.entity.Trash

// 아이템 클릭 시 실행될 동작을 외부에서 전달 받음
class TrashAdapter(private val onItemClick: (Trash) -> Unit) :
    ListAdapter<Trash, TrashAdapter.TrashViewHolder>(DIFF_CALLBACK) {

    private var selectedMemos = mutableSetOf<Int>() // 선택된 메모 리스트 (중복 방지)

    var isMultiSelect = false
        // isMultiSelect에 새로운 값이 할당될 때 자동 실행되는 setter
        set(value) {
            field = value // backing field(실제 저장되는 값)에 대입
            if (!value) selectedMemos.clear() // 다중 선택 모드 해제 시 체크 상태 초기화
            notifyDataSetChanged() // 전체 아이템 갱신
        }

    inner class TrashViewHolder(private val binding: ItemMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trash: Trash) {
            with(binding) {
                // Trash 데이터를 각 뷰에 할당
                tvContent.text = trash.content
                // 남은 보관일 계산 (삭제된 시각, 최대 보관일 30일)
                val daysLeft = calculateDaysLeft(trash.deletedAt, MAX_TRASH_DAYS)
                tvDate.apply {
                    text = context.getString(R.string.delete_after_day, daysLeft)
                    setTextColor(ContextCompat.getColor(context, R.color.orange))
                }
                imageView.visibility = View.GONE // 휴지통에서는 잠금 필요 없음

                // 유효한 아이템을 가리킬 때만 실행
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    checkBox.apply {
                        visibility = if (isMultiSelect) View.VISIBLE else View.GONE
                        setOnCheckedChangeListener(null) // 초기화 중 콜백 방지
                        isChecked = adapterPosition in selectedMemos // 선택 상태 반영

                        // 선택 상태 변경 시 selectedMemos에 추가/제거
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
                    if (isMultiSelect) {
                        setOnClickListener(null)
                    } else {
                        setOnClickListener {
                            onItemClick(trash)
                        }
                    }
                }
            }
        }
    }

    private fun calculateDaysLeft(deletedAt: Long, maxDays: Int): Int {
        // 삭제 후 경과일
        val daysPassed = ((System.currentTimeMillis() - deletedAt) / ONE_DAYS_MS).toInt()
        // 남은 보관일 (coerceAtLeast(0) : 계산 결과가 음수면 0으로 보정)
        return (maxDays - daysPassed).coerceAtLeast(0)
    }

    // ViewHolder 생성 및 아이템 뷰 초기화
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashAdapter.TrashViewHolder {
        val binding = ItemMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrashViewHolder(binding)
    }

    // 재사용되는 ViewHolder에 데이터 바인딩
    override fun onBindViewHolder(holder: TrashAdapter.TrashViewHolder, position: Int) {
        // 해당 위치의 데이터를 ViewHolder에 바인딩
        holder.bind(getItem(position))
    }

    companion object { // 클래스 수준의 정적 객체 (static처럼 동작)
        // RecyclerView 성능 최적화를 위해 변경 사항만 업데이트
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Trash>() {
            override fun areItemsTheSame(oldItem: Trash, newItem: Trash): Boolean {
                return oldItem.id == newItem.id
            }
            override fun areContentsTheSame(oldItem: Trash, newItem: Trash): Boolean {
                return oldItem == newItem
            }
        }
    }

    fun toggleSelectAll() {
        // (선택된 메모 수 = 전체 메모 수) 전체 선택이면
        if (selectedMemos.size == currentList.size) {
            selectedMemos.clear() // 전체 선택 해제
        } else { // 전체 선택이 아니면
            // 전체 선택
            with(selectedMemos) {
                clear()
                addAll(currentList.indices)
            }
        }
        notifyDataSetChanged()
    }

    fun getSelectedTrash() : List<Trash> {
        // selectedMemos 인덱스 순회 (null 제외)
        return selectedMemos.mapNotNull { index ->
            // 해당 인덱스의 아이템을 가져옴 (없으면 null)
            currentList.getOrNull(index)
        }
    }
}
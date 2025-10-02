package com.example.memorynotenew.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.memorynotenew.R
import com.example.memorynotenew.common.Constants
import com.example.memorynotenew.databinding.ItemMemoBinding
import com.example.memorynotenew.room.entity.Trash

class TrashAdapter : ListAdapter<Trash, TrashAdapter.TrashViewHolder>(DIFF_CALLBACK) {
    private var selectedMemos = mutableSetOf<Int>() // 선택한 메모 리스트

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
                tvContent.text = trash.content

                val deletedAt = trash.deletedAt // 삭제한 시각
                val maxDays = 30 // 휴지통 최대 보관일
                val current = System.currentTimeMillis() // 현재 시각
                // 삭제 후 경과 일수
                val daysPassed = ((current - deletedAt) / Constants.ONE_DAYS_MS).toInt()
                // 남은 보관 일수 (coerceAtLeast(0) : 계산 결과가 음수면 0으로 보정)
                val daysLeft = (maxDays - daysPassed).coerceAtLeast(0)
                val context = imageView.context
                tvDate.apply {
                    text = context.getString(R.string.days_left, daysLeft)
                    setTextColor(ContextCompat.getColor(context, R.color.orange))
                }
                imageView.visibility = View.GONE // 휴지통에서는 잠금 필요 없음

                // 유효한 어댑터 위치일 때만 실행
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    checkBox.apply {
                        visibility = if (isMultiSelect) View.VISIBLE else View.GONE
                        setOnCheckedChangeListener(null) // 불필요한 리스너 호출 방지
                        isChecked = adapterPosition in selectedMemos

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
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashAdapter.TrashViewHolder {
        val binding = ItemMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrashViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrashAdapter.TrashViewHolder, position: Int) {
        // 현재 위치의 Memo 데이터를 ViewHolder에 바인딩
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
        // 선택된 메모 수 = 전체 메모 수 -> 전체 선택이면
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

    // selectedMemos 인덱스 -> Trash 객체 리스트
    fun getSelectedMemos() : List<Trash> {
        // selectedMemos 인덱스 순회 (null 제외)
        return selectedMemos.mapNotNull { index ->
            // 해당 인덱스의 아이템을 가져옴 (없으면 null)
            currentList.getOrNull(index)
        }
    }
}
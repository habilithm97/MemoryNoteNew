package com.example.memorynotenew.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.memorynotenew.R
import com.example.memorynotenew.databinding.ItemMemoBinding
import com.example.memorynotenew.room.memo.Memo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 아이템 클릭 시 실행될 동작을 외부에서 전달 받음
class MemoAdapter(private val onItemClick: (Memo) -> Unit) :
    ListAdapter<Memo, MemoAdapter.MemoViewHolder>(DIFF_CALLBACK) {

    inner class MemoViewHolder(private val binding: ItemMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(memo: Memo) {
                binding.apply {
                    // Memo 데이터를 각 View에 할당
                    tvContent.text = memo.content
                    tvDate.text = SimpleDateFormat(itemView.context.getString(R.string.date_format),
                        Locale.getDefault()).format(Date(memo.date))
                    imageView.visibility = if (memo.isLocked) View.VISIBLE else View.INVISIBLE

                    root.setOnClickListener {
                        onItemClick(memo)
                    }
                }
            }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoAdapter.MemoViewHolder {
        val binding = ItemMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemoAdapter.MemoViewHolder, position: Int) {
        // 현재 위치의 Memo 데이터를 ViewHolder에 바인딩
        holder.bind(getItem(position))
    }
}
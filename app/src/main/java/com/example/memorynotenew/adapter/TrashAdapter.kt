package com.example.memorynotenew.adapter

import android.graphics.Color
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
                // 남은 보관 일수
                val daysLeft = (maxDays - daysPassed).coerceAtLeast(0)
                val context = imageView.context
                tvDate.apply {
                    text = context.getString(R.string.days_left, daysLeft)
                    setTextColor(ContextCompat.getColor(context, R.color.orange))
                }
                imageView.visibility = View.GONE // 휴지통에서는 잠금 필요 없음
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
}
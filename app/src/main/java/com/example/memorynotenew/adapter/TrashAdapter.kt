package com.example.memorynotenew.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.memorynotenew.databinding.ItemMemoBinding
import com.example.memorynotenew.room.entity.Trash

class TrashAdapter : ListAdapter<Trash, TrashAdapter.TrashViewHolder>(DIFF_CALLBACK) {

    inner class TrashViewHolder(private val binding: ItemMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trash: Trash) {
            with(binding) {
                tvContent.text = trash.content
                // 휴지통에서는 날짜, 잠금 필요 없음
                tvDate.visibility = View.GONE
                imageView.visibility = View.GONE
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
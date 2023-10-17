package com.example.trekbuddy.ui.guide

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trekbuddy.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class CourseAdapter(private val coursesList: List<Course>, private val googleMap: GoogleMap?):
    RecyclerView.Adapter<CourseAdapter.ViewHolder>(){
    private var selectedItemPosition: Int = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.courselist_item, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = coursesList[position]
        holder.courseName.text = course.courseName

        // 이미지를 Glide를 사용하여 로드하고 표시
        Glide.with(holder.itemView.context)
            .load(course.imageUrl)
            .into(holder.courseImage)

        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (selectedItemPosition != position) {
                // 이전에 선택된 항목이 있으면 선택을 해제하고 새 항목을 선택
                val previouslySelectedItemPosition = selectedItemPosition
                selectedItemPosition = position
                notifyItemChanged(previouslySelectedItemPosition)
                holder.itemView.setBackgroundColor(Color.YELLOW) // 선택한 항목의 배경색

            }
        }
        // 선택된 항목에 대한 배경색 설정
        if (position == selectedItemPosition) {
            holder.itemView.setBackgroundColor(Color.YELLOW) // 선택된 항목의 배경색
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE) // 선택되지 않은 항목의 배경색
        }
    }

    override fun getItemCount(): Int {
        return coursesList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.courseNameTextView)
        val courseImage: ImageView = itemView.findViewById(R.id.courseImageView)
    }
}
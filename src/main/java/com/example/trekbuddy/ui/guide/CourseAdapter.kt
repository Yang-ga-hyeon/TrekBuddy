package com.example.trekbuddy.ui.guide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trekbuddy.R

class CourseAdapter(private val coursesList: List<Course>):
    RecyclerView.Adapter<CourseAdapter.ViewHolder>(){

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
    }

    override fun getItemCount(): Int {
        return coursesList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.courseNameTextView)
        val courseImage: ImageView = itemView.findViewById(R.id.courseImageView)
    }
}
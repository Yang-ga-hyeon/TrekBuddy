package com.example.trekbuddy.ui.profile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R

class VisitAdapter(private val visitDataList: List<Visitlist>, private val clickListener: VisitFragment) : RecyclerView.Adapter<VisitAdapter.VisitViewHolder>() {


    // 뷰 홀더 클래스
    inner class VisitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logID: TextView = itemView.findViewById(R.id.logIDView)
        val courseName: TextView = itemView.findViewById(R.id.corsenameView)
        val places: TextView = itemView.findViewById(R.id.visitplacesView)
        val tags: TextView = itemView.findViewById(R.id.tagView)
        val time: TextView = itemView.findViewById(R.id.timeView)
        val date: TextView = itemView.findViewById(R.id.dateView)
        val shareButton: ImageView = itemView.findViewById(R.id.shareButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        // 아이템 뷰를 생성하고 뷰 홀더를 반환
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.viewholder_visit, parent, false)
        return VisitViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        // 아이템 데이터를 뷰 홀더의 뷰 요소에 연결
        val currentVisit = visitDataList[position]
        holder.logID.text = currentVisit.logID

        if (currentVisit.courseName.isNotEmpty()) {
            holder.courseName.text = "코스가 공유되었습니다!"
        } else {
            // courseName이 null일 경우 "코스를 공유해보세요!"로 설정
            holder.courseName.text = "코스를 공유해보세요!"
            holder.courseName.setTextColor(Color.parseColor("#CCCCCC"))
        }
        holder.places.text = currentVisit.places?.joinToString(" -> ")
        holder.date.text = currentVisit.date

        val time = currentVisit.time
        if (time >= 60){
            val hour = time / 60
            val min = time % 60
            holder.time.text = hour.toString() + "시간" + min.toString() + "분"
        }
        else{
            holder.time.text = currentVisit.time.toString() + "분"
        }
        holder.tags.text = currentVisit.tags.joinToString(prefix=" #", separator = " #")

        holder.shareButton.setOnClickListener {
            clickListener.onShareButtonClicked(currentVisit)
        }





    }

    override fun getItemCount(): Int {
        return visitDataList.size
    }





}

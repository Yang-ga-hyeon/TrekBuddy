package com.example.trekbuddy.ui.profile
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R



class ReviewAdapter(private val context: Context, private val reviewDataList: List<Reviewlist>, private val tagSelectionListener: OnTagSelectionListener) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>(){

    private val selectedTagsList: MutableList<String> = mutableListOf()

    // 뷰 홀더 클래스
    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.placeView)
        val tags: List<TextView> = List(8) { itemView.findViewById(context.resources.getIdentifier("tagView${it + 1}", "id", context.packageName)) }
        val tagEdit: EditText = itemView.findViewById(R.id.tagEditText)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewAdapter.ReviewViewHolder {
        // 아이템 뷰를 생성하고 뷰 홀더를 반환
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.viewholder_review, parent, false)
        return ReviewViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ReviewAdapter.ReviewViewHolder, position: Int) {
        val item = reviewDataList[position]
        holder.placeName.text = item.placeName

        for (i in 0 until item.tags.size) {
            if (i < 8) {
                val tagTextView = holder.tags[i]
                if (tagTextView != null) {
                    val tagWithHash = "#" + item.tags[i]
                    tagTextView.text = tagWithHash
                    // 클릭 시 배경색 변경
                    // 클릭 시 배경색 변경
                    tagTextView.setOnClickListener {
                        val tag = item.tags[i]
                        if (tagTextView.isSelected) {
                            // 클릭을 해제하면 원래 배경색으로
                            tagTextView.setBackgroundColor(Color.parseColor("#FFF0F8FF"))
                            // 선택한 태그 목록에서 제거
                            selectedTagsList.remove(tag)
                        } else {
                            // 클릭 시 배경색 변경
                            tagTextView.setBackgroundColor(Color.parseColor("#FFFFD700"))
                            // 선택한 태그를 전달
                            selectedTagsList.add(tag)
                        }
                        // 클릭 상태를 변경
                        tagTextView.isSelected = !tagTextView.isSelected

                        // 선택한 태그 목록을 ReviewFragment로 전달
                        tagSelectionListener.onTagsSelected(selectedTagsList)
                    }


                }
            }
        }

        val tagEditTextView = holder.tagEdit
        if (tagEditTextView != null) {
            tagEditTextView.setText(item.tagEdit ?: "")
        }


    }

    override fun getItemCount(): Int {
        return reviewDataList.size
    }


}
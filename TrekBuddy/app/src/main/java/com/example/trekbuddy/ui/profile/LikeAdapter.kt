package com.example.trekbuddy.ui.profile
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.ui.SearchCourse
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LikeAdapter(private val courses: List<SearchCourse>) : RecyclerView.Adapter<LikeAdapter.LikeViewHolder>() {
    inner class LikeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseNameTextView: TextView = itemView.findViewById(R.id.courseNameTextView)
        val placesTextView: TextView = itemView.findViewById(R.id.placesTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val tagTextView: TextView = itemView.findViewById(R.id.tagTextView)
        val likeNoButton: ImageView = itemView.findViewById(R.id.likeNoButton)
        val likeYesButton: ImageView = itemView.findViewById(R.id.likeYesButton)
    }
    val db = Firebase.firestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.course_search_like, parent, false)
        return LikeViewHolder(view)
    }
    override fun onBindViewHolder(holder: LikeViewHolder, position: Int) {
        val course = courses[position]
        holder.courseNameTextView.text = course.name
        holder.placesTextView.text = course.places.joinToString(" -> ")
        val time = if (course.time.isNotBlank()) {
            course.time.toInt()
        } else {
            0 // 빈 문자열("")인 경우 0으로 처리하거나 다른 기본값을 사용할 수 있음
        }
        if (time >= 60){
            val hour = time / 60
            val min = time % 60
            holder.timeTextView.text = hour.toString() + "시간" + min.toString() + "분"
        }
        else{
            holder.timeTextView.text = course.time + "분"
        }
        holder.tagTextView.text = course.tags.joinToString(prefix=" #", separator = " #")
        // '좋아요' 상태에 따라 버튼 상태 수정
        if (course.isLiked){
            holder.likeYesButton.visibility = View.VISIBLE
            holder.likeNoButton.visibility = View.INVISIBLE
            holder.likeYesButton.setOnClickListener{ unlikeCourse(course) }
        }
        else{
            holder.likeYesButton.visibility = View.INVISIBLE
            holder.likeNoButton.visibility = View.VISIBLE
            holder.likeNoButton.setOnClickListener{ likeCourse(course) }
        }
    }
    private fun likeCourse(course: SearchCourse) {
        val userId = course.userID
        val likesRef = db.collection("Likes")

        val placeNameAsyncJobs = mutableListOf<Deferred<String?>>()

        for (placeName in course.places) {
            val placeQuery = db.collection("PlaceList").whereEqualTo("name", placeName)
            val placeNameJob = GlobalScope.async(Dispatchers.IO) {
                val placeQuerySnapshot = placeQuery.get().await()
                if (!placeQuerySnapshot.isEmpty) {
                    val placeDoc = placeQuerySnapshot.documents[0]
                    return@async placeDoc.id // This will get the placeID
                }
                return@async null
            }
            placeNameAsyncJobs.add(placeNameJob)
        }

        GlobalScope.launch(Dispatchers.Main) {
            val placeIDs = placeNameAsyncJobs.awaitAll().filterNotNull()

            val likeData = hashMapOf(
                "userID" to userId,
                "courseName" to course.name,
                "places" to placeIDs
            )

            likesRef.add(likeData)
                .addOnSuccessListener {
                    course.isLiked = true
                    notifyDataSetChanged()
                }
                .addOnFailureListener { }
        }
    }

    private fun unlikeCourse(course: SearchCourse) {
        val userId = course.userID
        val likesRef = db.collection("Likes")
        val query = likesRef.whereEqualTo("userID", userId)
            .whereEqualTo("courseName", course.name)

        query.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // 해당 문서를 삭제하여 좋아요 취소
                    likesRef.document(document.id).delete()
                        .addOnSuccessListener {
                            // 좋아요가 성공적으로 제거됐을 때 수행할 작업
                            course.isLiked = false
                            notifyDataSetChanged()
                        }
                        .addOnFailureListener { }
                }
            }
            .addOnFailureListener {  }
    }
    override fun getItemCount(): Int {
        return courses.size
    }
}

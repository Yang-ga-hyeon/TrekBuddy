package com.example.trekbuddy.ui.guide

import android.graphics.Color
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore

class CourseAdapter(private val coursesList: List<Course>, private val googleMap: GoogleMap?, private val locationDataListener: LocationDataListener):
    RecyclerView.Adapter<CourseAdapter.ViewHolder>(){
    private var selectedItemPosition: Int = RecyclerView.NO_POSITION
    private val locationArrayList = ArrayList<LatLng>()
    val locationNames = ArrayList<String>()


    // Firestore에서 Geopoint 데이터를 읽어오고 지도에 마커를 표시
    private fun loadGeoPointsFromFirestore(coursePosition: Int) {

        // Firestore 인스턴스를 가져옵니다.
        val db = FirebaseFirestore.getInstance()
        val courseNumber = coursePosition+1
        val courseName = "course$courseNumber"

        // "SystemCourseList" 컬렉션의 위치를 가져옵니다.
        val collectionRef = db.collection("SystemCourseList")

        collectionRef.document(courseName).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val places = document["places"] as List<String>
                locationArrayList.clear() // 기존 데이터를 지우고 새로운 데이터로 대체
                locationNames.clear()

                    // PlaceList에서 위치 데이터를 가져옵니다.
                    for (place in places) {
                        val placeRef = db.collection("PlaceList").document(place)
                        placeRef.get()
                            .addOnSuccessListener { placeDocument ->
                                val geoPoint = placeDocument.getGeoPoint("coordinate")
                                val locName = placeDocument.getString("name")
                                if (geoPoint != null) {
                                    val location = LatLng(geoPoint.latitude, geoPoint.longitude)
                                    locationArrayList.add(location)
                                }
                                if (locName != null) {
                                    locationNames.add(locName)
                                }
                                locationDataListener.onLocationDataLoaded(locationArrayList, locationNames)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FirestoreError", "Error loading Firestore data: $exception")
                            }
                    }
                } else {
                    Log.d("FirestoreError", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error loading Firestore data: $exception")
            }
    }

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

                loadGeoPointsFromFirestore(position)
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
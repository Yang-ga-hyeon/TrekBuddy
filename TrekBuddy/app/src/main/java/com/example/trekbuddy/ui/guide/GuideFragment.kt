package com.example.trekbuddy.ui.guide

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentGuideBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore


class GuideFragment : Fragment() {

    private var _binding: FragmentGuideBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CourseAdapter
    private var googleMap: GoogleMap? = null
    val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentGuideBinding.inflate(inflater, container, false)
        val root: View = binding.root
        recyclerView = root.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Firebase에서 데이터를 가져와서 Adapter에 전달
        val coursesList = ArrayList<Course>() // Firebase에서 가져온 데이터


        // Firestore에서 데이터를 가져와 RecyclerView에 표시
        db.collection("CourseList")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val coursesList = ArrayList<Course>()
                for (document in querySnapshot) {
                    val courseName = document["name"].toString()
                    val imageUrl = document["image"].toString()
                    val latitude = document["latitude"].toString()
                    val longitude = document["longitude"].toString()
                    val locationName = document["name"].toString()

                    if (courseName != null && imageUrl != null) {
                        val course = Course(null, courseName, imageUrl, latitude, longitude, locationName)
                        coursesList.add(course)
                    }
                }
                adapter = CourseAdapter(coursesList, googleMap)
                recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // 오류 처리
            }

        return root
    }

    private val callback = OnMapReadyCallback { googleMap ->
        // 파이어베이스 컬렉션 참조
        val collectionRef = db.collection("CourseList")

        // 데이터 가져오기
        collectionRef.get()
            .addOnSuccessListener { documents ->
                val locations = ArrayList<LatLng>() // LatLng의 배열로 장소의 위치를 저장

                for (document in documents) {
                    // Firestore 문서에서 위도와 경도 필드 가져오기
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    val locationName = document.getString("name")

                    if (latitude != null && longitude != null && locationName != null) {
                        val location = LatLng(latitude, longitude)

                        googleMap.addMarker(MarkerOptions().position(location).title(locationName))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
                        locations.add(location)
                    }
                }

            }
            .addOnFailureListener { exception ->
                // 오류 처리
                Log.e("FirebaseError", "Error getting documents: ${exception.message}")
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
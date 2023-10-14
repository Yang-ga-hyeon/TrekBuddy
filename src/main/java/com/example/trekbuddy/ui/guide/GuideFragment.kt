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
import com.google.firebase.firestore.FirebaseFirestore


class GuideFragment : Fragment() {

    private var _binding: FragmentGuideBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CourseAdapter

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

                    if (courseName != null && imageUrl != null) {
                        val course = Course(null, courseName, imageUrl)
                        coursesList.add(course)
                    }
                }
                adapter = CourseAdapter(coursesList)
                recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // 오류 처리
            }

        return root
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}
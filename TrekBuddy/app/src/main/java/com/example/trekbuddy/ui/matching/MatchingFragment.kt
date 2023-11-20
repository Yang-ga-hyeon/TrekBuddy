package com.example.trekbuddy.ui.matching

import MatchingAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.*
import android.widget.EditText
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentMatchingBinding
import com.example.trekbuddy.ui.SearchCourse
import com.example.trekbuddy.ui.guide.GuideFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class MatchingFragment : Fragment(), onItemListListener {
    private var _binding: FragmentMatchingBinding? = null
    private val binding get() = _binding!!
    private lateinit var SearchView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageView
    private lateinit var searchResetButton: ImageView

    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val currentUser: FirebaseUser? = auth.currentUser
    val userId = currentUser?.email.toString()
    var searchClicked = false

    // 검색 결과 데이터
    private val searchResults = mutableListOf<SearchCourse>() // Course 클래스는 이전에 작성한 대로
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMatchingBinding.inflate(inflater, container, false)
        val root: View = binding.root
        SearchView = root.findViewById(R.id.SearchResultsRecyclerView)
        // RecyclerView 초기화
        val adapter = MatchingAdapter(searchResults, this)
        SearchView.adapter = adapter
        SearchView.layoutManager = LinearLayoutManager(requireContext())

        // 검색 버튼 클릭 리스너
        searchEditText = root.findViewById(R.id.searchText)
        searchButton = root.findViewById(R.id.searchButton)
        searchResetButton = root.findViewById(R.id.searchResetButton)

        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString()
            if (searchText.isNotEmpty()) {
                // 검색어가 작성되었을 때
                searchClicked = true
                searchCourses(searchText)
                searchButton.visibility = View.GONE
                searchResetButton.visibility = View.VISIBLE
            } else {
                searchClicked = false
            }
        }
        searchResetButton.setOnClickListener {
            // 초기화 버튼 클릭 시
            searchEditText.text = null
            searchClicked = false
            searchResults.clear()
            searchButton.visibility = View.VISIBLE
            searchResetButton.visibility = View.GONE
            SearchView.adapter?.notifyDataSetChanged()
        }
        return root
    }

    private fun findLikedLog(userID: String, courseName: String, callback: (Boolean) -> Unit) {
        val likeCourseRef = db.collection("Likes")
        val likedCourseQuery =
            likeCourseRef.whereEqualTo("userID", userID).whereEqualTo("courseName", courseName)

        likedCourseQuery.get().addOnSuccessListener { documents ->
            callback(!documents.isEmpty)
        }
    }

    private fun searchCourses(searchText: String) {
        // 검색 조건에 맞게 SystemCourseList 및 UserCourseList에서 데이터 가져오기
        val systemCourseRef = db.collection("SystemCourseList")
        val userCourseRef = db.collection("UserCourseList")
        val placeListRef = db.collection("PlaceList")
        // 각 코스 리스트 내 배열에서 여쭤보기
        val placeNameAsyncJobs = mutableListOf<Deferred<List<String>>>()
        val placeListQuery = placeListRef.whereEqualTo("name", searchText)
        placeListQuery.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val placeID = document.id
                val systemCourseNameQuery = systemCourseRef.whereArrayContains("places", placeID)
                val userCourseNameQuery = userCourseRef.whereArrayContains("places", placeID)
                systemCourseNameQuery.get().addOnSuccessListener { systemCourseDocuments ->
                    for (systemCourseDocument in systemCourseDocuments) {
                        val courseId = systemCourseDocument.id
                        val name = systemCourseDocument.getString("name") ?: ""
                        // 중복 "placeID"가 있을 수 있으므로 전부 가져옴
                        val placeIDs = systemCourseDocument.get("places") as? List<String> ?: emptyList()
                        val time = systemCourseDocument.get("time") ?: ""
                        val tags = systemCourseDocument.get("tags") as? List<String> ?: emptyList()

                        // 모든 "placeID"에 대한 이름을 가져오고, 중복을 고려하지 않음
                        val placeNameAsyncJobs = placeIDs.map { id ->
                            GlobalScope.async(Dispatchers.IO) {
                                val placeDocRef = db.collection("PlaceList").document(id)
                                val placeDocSnapshot = placeDocRef.get().await()
                                if (placeDocSnapshot.exists()) {
                                    placeDocSnapshot.getString("name")
                                } else {
                                    null
                                }
                            }
                        }

                        GlobalScope.launch(Dispatchers.Main) {
                            val placeNames = placeNameAsyncJobs.awaitAll().filterNotNull()
                            val places = placeNames.toMutableList()

                            findLikedLog(userId.toString(), name) { isLiked ->
                                val course = SearchCourse(
                                    courseId,
                                    name,
                                    places,
                                    time.toString(),
                                    tags,
                                    isLiked,
                                    userId
                                )
                                if (!searchResults.contains(course)) {
                                    searchResults.add(course)
                                    SearchView.adapter?.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                    // 결과 처리
                }
                userCourseNameQuery.get().addOnSuccessListener { userCourseDocuments ->
                    GlobalScope.launch(Dispatchers.Main) {
                        for (userCourseDocument in userCourseDocuments) {
                            val placeNameAsyncJobs = mutableListOf<Deferred<String?>>() // 각 문서마다 새로운 리스트를 생성

                            val courseId = userCourseDocument.id
                            val name = userCourseDocument.getString("name") ?: ""
                            val placeID = userCourseDocument.get("places") as? List<String> ?: emptyList()
                            val time = userCourseDocument.get("time") ?: ""
                            val tags = userCourseDocument.get("tags") as? List<String> ?: emptyList()

                            // 각 places ID를 place 이름으로 변환
                            for (id in placeID) {
                                val placeDocRef = db.collection("PlaceList").document(id)
                                val placeNameJob = GlobalScope.async(Dispatchers.IO) {
                                    val placeDocSnapshot = placeDocRef.get().await()
                                    if (placeDocSnapshot.exists()) {
                                        placeDocSnapshot.getString("name")
                                    } else {
                                        null
                                    }
                                }
                                placeNameAsyncJobs.add(placeNameJob)
                            }

                            val placeNames = placeNameAsyncJobs.awaitAll().filterNotNull()
                            val places = placeNames.toMutableList()

                            findLikedLog(userId.toString(), name) { isLiked ->
                                val course = SearchCourse(
                                    courseId,
                                    name,
                                    places,
                                    time.toString(),
                                    tags,
                                    isLiked,
                                    userId
                                )
                                if (!searchResults.contains(course)) {
                                    searchResults.add(course)
                                    SearchView.adapter?.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }.addOnFailureListener { exception ->
                    Log.w("MatchingFragment", "Error getting documents: ", exception)
                }
            }
        }

        val systemCourseTagQuery = systemCourseRef.whereArrayContains("tags", searchText)
        val userCourseTagQuery = userCourseRef.whereArrayContains("tags", searchText)
        val userId = currentUser?.email
        systemCourseTagQuery.get().addOnSuccessListener { documents ->
            val placeNameAsyncJobs = mutableListOf<Deferred<String?>>()
            for (document in documents) {
                val courseId = document.id
                val name = document.getString("name") ?: ""
                val placeID = document.get("places") as? List<String> ?: emptyList()
                val time = document.get("time") ?: ""
                val tags = document.get("tags") as? List<String> ?: emptyList()

                // places ID를 place 이름으로 변환
                for (id in placeID) {
                    val placeDocRef = db.collection("PlaceList").document(id)
                    val placeNameJob = GlobalScope.async(Dispatchers.IO) {
                        val placeDocSnapshot = placeDocRef.get().await()
                        if (placeDocSnapshot.exists()) {
                            placeDocSnapshot.getString("name")
                        } else {
                            null
                        }
                    }
                    placeNameAsyncJobs.add(placeNameJob)
                }
                GlobalScope.launch(Dispatchers.Main) {
                    val placeNames = placeNameAsyncJobs.awaitAll().filterNotNull()
                    val places = placeNames.toMutableList()

                    findLikedLog(userId.toString(), name) { isLiked ->
                        val course = SearchCourse(
                            courseId,
                            name,
                            places,
                            time.toString(),
                            tags,
                            isLiked,
                            userId
                        )
                        if (!searchResults.contains(course)) {
                            searchResults.add(course)
                            SearchView.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
        userCourseTagQuery.get().addOnSuccessListener { documents ->
            GlobalScope.launch(Dispatchers.Main) {
                for (document in documents) {
                    val courseId = document.id
                    val name = document.getString("name") ?: ""
                    val placeID = document.get("places") as? List<String> ?: emptyList()
                    val time = document.get("time") ?: ""
                    val tags = document.get("tags") as? List<String> ?: emptyList()

                    val placeNames = mutableListOf<String>()
                    for (id in placeID) {
                        val placeDocRef = db.collection("PlaceList").document(id)
                        try {
                            val placeDocSnapshot = placeDocRef.get().await()
                            if (placeDocSnapshot.exists()) {
                                val placeName = placeDocSnapshot.getString("name")
                                if (placeName != null) {
                                    placeNames.add(placeName)
                                }
                            }
                        } catch (e: Exception) {
                            // 예외 처리: 필요에 따라 추가하세요.
                        }
                    }

                    findLikedLog(userId.toString(), name) { isLiked ->
                        val course = SearchCourse(
                            courseId,
                            name,
                            placeNames.toMutableList(),
                            time.toString(),
                            tags,
                            isLiked,
                            userId
                        )

                        if (!searchResults.contains(course)) {
                            searchResults.add(course)
                        }

                        // 모든 문서 처리 후에 한 번만 UI를 업데이트합니다.
                        if (document == documents.last()) {
                            SearchView.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.w("MatchingFragment", "Error getting documents: ", exception)
        }



    }

    override fun onItemClicked(course: SearchCourse) {
        val navController = findNavController()

        val bundle = bundleOf("SearchCourse" to course, "currentUserId" to userId)
        navController.navigate(R.id.navigation_guide, bundle)
    }
}


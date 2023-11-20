package com.example.trekbuddy.ui.profile
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentLikesBinding
import com.example.trekbuddy.ui.SearchCourse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class LikeFragment : Fragment() {
    private var _binding: FragmentLikesBinding? = null
    private val binding get() = _binding!!
    private lateinit var LikesView: RecyclerView
    private lateinit var GoBackButton: ImageView

    private val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val currentUser: FirebaseUser? = auth.currentUser
    //like 조회 결과 데이터
    private val likeLogs = mutableListOf<SearchCourse>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLikesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        LikesView = root.findViewById(R.id.likeslistView)
        // RecyclerView 초기화
        val adapter = LikeAdapter(likeLogs)
        LikesView.adapter = adapter
        LikesView.layoutManager = LinearLayoutManager(requireContext())

        //뒤로 가기 버튼
        GoBackButton = root.findViewById(R.id.GoBackButton)
        GoBackButton.setOnClickListener {
            // ProfileFragment로 이동
            findNavController().navigateUp()
        }
        loadLikeDataFromFirebase(currentUser?.email.toString())

        return root
    }

    private fun loadLikeDataFromFirebase(userID: String) {
        val likeRef = db.collection("Likes")
        val likeRefQuery = likeRef.whereEqualTo("userID", userID)

        likeRefQuery.get()
            .addOnSuccessListener { querySnapshot ->
                likeLogs.clear()

                val systemCourseDocRef = db.collection("SystemCourseList")
                val userCourseDocRef = db.collection("UserCourseList")
                for (document in querySnapshot) {
                    val placeNameAsyncJobs = mutableListOf<Deferred<String?>>()
                    val courseName = document.getString("courseName") ?: ""
                    val placeID = document["places"] as? List<String> ?: emptyList()
                    var places = mutableListOf<String>()
                    var tags = emptyList<String>()
                    var courseID = ""
                    var time = ""
                    val course = SearchCourse(courseId = "", name = courseName, places, time, tags, false, userID)
                    // Create an async job for each placeName retrieval
                    for (id in placeID) {

                        val placeDocRef = db.collection("PlaceList").document(id)
                        val placeNameJob = GlobalScope.async(Dispatchers.IO) {
                            val placeDocSnapshot = placeDocRef.get().await()
                            if (placeDocSnapshot.exists()) {
                                return@async placeDocSnapshot.getString("name")
                            }
                            return@async null
                        }
                        placeNameAsyncJobs.add(placeNameJob)
                    }

                    // Launch a coroutine to handle placeName retrieval results
                    GlobalScope.launch(Dispatchers.Main) {
                        // Wait for all placeName retrieval jobs to complete
                        val placeNames = placeNameAsyncJobs.awaitAll().filterNotNull()

                        // Query systemCourseDocRef
                        val systemCourseQuery = systemCourseDocRef.whereEqualTo("name", courseName).get().await()
                        if (!systemCourseQuery.isEmpty) {
                            val q = systemCourseQuery.documents[0]
                            courseID = q.getString("courseID") ?: ""
                            time = q.getLong("time")?.toString() ?: ""
                            tags = q.get("tags") as? List<String> ?: emptyList()
                        }

                        // Query userCourseDocRef if necessary
                        if (courseID.isEmpty()) {
                            val userCourseQuery = userCourseDocRef.whereEqualTo("name", courseName).get().await()
                            if (!userCourseQuery.isEmpty) {
                                val q = userCourseQuery.documents[0]
                                courseID = q.getString("courseID") ?: ""
                                time = q.getLong("time")?.toString() ?: ""
                                tags = q.get("tags") as? List<String> ?: emptyList()
                            }
                        }

                        course.courseId = courseID
                        course.time = time
                        course.places = placeNames
                        course.tags = tags
                        course.isLiked = true

                        if (!likeLogs.contains(course)) {
                            likeLogs.add(course)
                            LikesView.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle the error
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
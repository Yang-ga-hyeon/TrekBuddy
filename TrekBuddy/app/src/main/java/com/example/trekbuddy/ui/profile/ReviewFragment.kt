package com.example.trekbuddy.ui.profile


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentReviewBinding
import com.google.firebase.firestore.FirebaseFirestore
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener



class ReviewFragment : Fragment(), OnTagSelectionListener {
    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReviewAdapter
    private val db = FirebaseFirestore.getInstance()
    private var logID: String? = null
    private var logTime: Int? = null
    private var selectedTagsList: List<String> = mutableListOf()
    private var logPlaceList: List<String> = mutableListOf()
    private var userID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val visitData = arguments?.getSerializable("visitData") as? Visitlist
        logID = visitData?.logID
        logTime = visitData?.time?.toInt()
        userID = arguments?.getString("currentUserId")
        println(userID)


        recyclerView = root.findViewById(R.id.reviewlistView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Firebase에서 데이터를 가져와서 Adapter에 전달
        loadReviewDataFromFirebase()

        //공유 버튼 클릭 리스너
        val shareButton2 = root.findViewById<Button>(R.id.shareButton2)
        shareButton2.setOnClickListener {
            println("클릭 정상")
            val courseNameEditText = root.findViewById<EditText>(R.id.courseNameEditText)
            val courseName = courseNameEditText.text.toString()

            // tagEditText에서 사용자 지정 태그를 가져옴
            val tagEditText = root.findViewById<EditText>(R.id.tagEditText)
            val customTag = tagEditText.text.toString()

            // selectedTagsList에서 선택된 태그를 가져옴
            var selectedTags = selectedTagsList
            if (customTag.isNotEmpty()) {
                selectedTags += customTag
            }

            val userCourseData = hashMapOf(
                "name" to courseName,
                "places" to logPlaceList,
                "tags" to selectedTags, // 사용자 지정 태그와 선택된 태그를 합침
                "time" to logTime,
                "userID" to userID
            )

            // Firestore의 UserCourseList 컬렉션에 데이터 추가
            db.collection("UserCourseList")
                .add(userCourseData)
                .addOnSuccessListener { documentReference ->
                    // 데이터 추가 성공 시 처리
                    println("데이터베이스에 입력 성공")
                    // "코스 리뷰가 등록되었습니다." Toast 메시지
                    Toast.makeText(requireContext(), "코스 리뷰가 등록되었습니다.", Toast.LENGTH_LONG).show()

                }
                .addOnFailureListener { e ->
                    // 데이터 추가 실패 시 처리

                }

            val logRef = db.collection("Log").document(logID ?: "")
            logRef.get()
                .addOnSuccessListener { logDocument ->
                    if (logDocument.exists()) {
                        val logname = logDocument.getString("logname")
                        if (logname != null) {
                            // "logname" 필드의 내용을 courseName으로 업데이트
                            val courseName = courseNameEditText.text.toString()
                            val updatedData = hashMapOf(
                                "logname" to courseName
                            )
                            logRef.update(updatedData as Map<String, Any>)
                                .addOnSuccessListener {
                                    // 업데이트 성공 시 처리
                                    println("logname 필드가 courseName으로 업데이트되었습니다.")
                                }
                                .addOnFailureListener { e ->
                                    // 업데이트 실패 시 처리
                                    println("logname 필드 업데이트 실패: $e")
                                }
                        }

                    }
                }


        }
        // closeButton 클릭 리스너
        val closeButton = root.findViewById<Button>(R.id.closeButton)
        closeButton.setOnClickListener {
            // 데이터 변경 여부를 Visit Fragment에 전달
            setFragmentResult("reviewResult", bundleOf("dataChanged" to true))
            findNavController().navigateUp()
        }

        return root
    }


    override fun onTagsSelected(selectedTags: List<String>) {
        selectedTagsList = selectedTags

    }

    private fun loadReviewDataFromFirebase() {
        val reviewList = mutableListOf<Reviewlist>()
        val logRef = db.collection("Log").document(logID ?: "")
        logRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val placesID = document.get("places") as? List<String>
                    if (placesID != null) {
                        logPlaceList = placesID
                    }
                    if (placesID != null) {
                        for (placeID in placesID) {
                            fetchPlaceName(placeID) { placeName ->
                                val tags = mutableListOf<String>()
                                fetchRandomTags(8) { randomTags ->
                                    tags.addAll(randomTags)
                                    println(tags)

                                    val reviewItem = Reviewlist(placeName, tags, null)
                                    reviewList.add(reviewItem)

                                    adapter = ReviewAdapter(requireContext(), reviewList, this) // reviewDataList 추가
                                    recyclerView.adapter = adapter


                                }

                            }

                        }
                    }
                } else {
                    // 해당 document가 존재하지 않을 때 처리
                }


            }
            .addOnFailureListener { exception ->
                // 데이터 가져오기 실패 시 처리
            }
    }

    private fun fetchPlaceName(placeID: String, callback: (String) -> Unit) {
        val placeRef = db.collection("PlaceList").document(placeID)

        placeRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val placeName = documentSnapshot.getString("name")
                    if (placeName != null) {
                        callback(placeName)
                    }
                } else {
                    // 해당 document가 존재하지 않을 때 처리
                }
            }
            .addOnFailureListener { exception ->
                // 데이터 가져오기 실패 시 처리
                // 예를 들어, 오류 메시지 출력
            }
    }

    private fun fetchRandomTags(count: Int, callback: (List<String>) -> Unit) {
        val tagListRef = db.collection("TagList")

        tagListRef.get()
            .addOnSuccessListener { querySnapshot ->
                val allTags = mutableListOf<String>()

                for (document in querySnapshot.documents) {
                    val tag = document.getString("expression")
                    if (tag != null) {
                        allTags.add(tag)
                    }
                }

                if (allTags.size >= count) {
                    val randomTags = allTags.shuffled().take(count)
                    callback(randomTags)
                }
            }
            .addOnFailureListener { exception ->
                // 데이터 가져오기 실패 시 처리
                // 예를 들어, 오류 메시지 출력
            }
    }




    companion object {
        // newInstance 메서드를 추가
        fun newInstance(visitData: Visitlist, currentUserId: String?): ReviewFragment {
            val fragment = ReviewFragment()
            val args = Bundle()
            // visitData 객체를 ReviewFragment에 전달
            args.putSerializable("visitData", visitData)
            args.putString("currentUserId", currentUserId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
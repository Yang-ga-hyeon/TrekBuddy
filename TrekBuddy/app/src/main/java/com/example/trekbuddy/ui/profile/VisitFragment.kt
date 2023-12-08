package com.example.trekbuddy.ui.profile

import kotlinx.coroutines.*
import android.os.Bundle
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentVisitBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener


class VisitFragment : Fragment(), OnShareButtonClickListener {

    private var _binding: FragmentVisitBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VisitAdapter
    private val db = FirebaseFirestore.getInstance()
    private var currentUserId: String? = null



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentVisitBinding.inflate(inflater, container, false)
        val root: View = binding.root
        //어댑터 연결
        recyclerView = root.findViewById(R.id.visitlistView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // 사용자 데이터를 가져오기
        currentUserId = arguments?.getString("currentUserId")
        println("visit test")
        println(currentUserId)

        setFragmentResultListener("reviewResult") { _, result ->
            val dataChanged = result?.getBoolean("dataChanged", false) ?: false
            if (dataChanged) {
                // 데이터가 변경되었을 때 RecyclerView를 다시 로드
                println("새로고침")
                // 1. 코루틴을 사용하여 호출
                CoroutineScope(Dispatchers.Main).launch {
                    loadVisitDataFromFirebase()
                }
            }
        }

        // 2. 코루틴을 사용하여 호출
        CoroutineScope(Dispatchers.Main).launch {
            loadVisitDataFromFirebase()
        }


        val backButton = root.findViewById<ImageView>(R.id.backButton)
        // backButton의 클릭 이벤트 리스너
        backButton.setOnClickListener {
            // ProfileFragment로 이동
            findNavController().navigate(R.id.action_navigation_visit_to_navigation_profile)
        }

        return root
    }



    override fun onShareButtonClicked(visitData: Visitlist) {
        // 코스가 이미 공유된 경우 다시 등록 및 수정 불가
        if (visitData.courseName.isNotEmpty()) {
            // Show a pop-up dialog
            showShareDialog(visitData.courseName)
        } else {
            // 클릭 이벤트에서 전달된 데이터(visitData)를 이용하여 ReviewFragment로 화면 전환
            val reviewFragment = ReviewFragment()
            val args = Bundle() // 전달할 데이터를 담은 Bundle을 생성
            args.putSerializable("visitData", visitData) // visitData는 Serializable이어야 합니다.
            args.putString("currentUserId", currentUserId)
            reviewFragment.arguments = args

            // NavController를 사용하여 ReviewFragment로 이동
            findNavController().navigate(R.id.action_navigation_visit_to_navigation_review, args)
        }
    }

    override fun showShareDialog(courseName: String) {
        // 팝업창 만들기
        AlertDialog.Builder(requireContext())
            .setTitle("[안내]")
            .setMessage("이미 등록된 코스입니다: $courseName")
            .setPositiveButton("확인") { _, _ ->
//                //토스트
//                Toast.makeText(requireContext(), "Sharing $courseName", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("닫기") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private suspend fun getPlaceName(placeId: String): String {
        val placeDocRef = db.collection("PlaceList").document(placeId)
        return try {
            val placeDocSnapshot = placeDocRef.get().await()
            if (placeDocSnapshot.exists()) {
                placeDocSnapshot["name"] as? String ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            // Handle exceptions appropriately
            e.printStackTrace()
            ""
        }
    }

    private suspend fun loadVisitDataFromFirebase() {
        // Firebase에서 데이터를 가져와 VisitAdapter에 전달
        try {
            val querySnapshot = db.collection("Log")
                .whereEqualTo("userID", currentUserId) // userID 필드와 값이 일치하는 문서만 선택
                .get()
                .await()

            val visitList = mutableListOf<Visitlist>()

            for (document in querySnapshot) {
                val logID = document.id
                val courseName = document["logname"].toString()
                val placeID = document["places"] as? List<String> ?: emptyList()
                val places = mutableListOf<String>()

                for (id in placeID) {
                    val placeName = getPlaceName(id)
                    places.add(placeName)
                }

                val starts = document["starts"] as? List<com.google.firebase.Timestamp> ?: emptyList()
                val startTimeMillis = starts[0].toDate().time
                val endTimeMillis = starts[starts.size - 1].toDate().time
                val timeDifferenceMillis = endTimeMillis - startTimeMillis
                val minutes = (timeDifferenceMillis / (1000 * 60)).toInt() // Milliseconds to minutes
                val time = minutes

                val tagID = mutableListOf<String>()
                runBlocking {
                    val tasks = placeID.map { place ->
                        GlobalScope.async {
                            val docRef = db.collection("PlaceList").document(place)
                            val document = docRef.get().await()
                            if (document != null) {
                                val tagList = document["tag"] as? List<String> ?: emptyList()
                                tagID.addAll(tagList)
                            }
                        }
                    }
                    // 모든 작업을 기다림
                    tasks.forEach { it.await() }
                    // 모든 데이터가 도착한 후에 tagID 목록을 사용할 수 있음
                    println(tagID.size)
                }
                println(tagID.size)

                val tags = mutableListOf<String>()
                runBlocking {
                    val tasks = tagID.map { tagid ->
                        GlobalScope.async {
                            if (!tagid.isNullOrEmpty()) { // tagid가 null 또는 빈 문자열이 아닌지 확인
                                val tagdocRef = db.collection("TagList").document(tagid)
                                val document = tagdocRef.get().await()
                                if (document != null) {
                                    val expression = document["expression"].toString()
                                    tags.add(expression)
                                }
                            }
                        }
                    }
                    tasks.forEach { it.await() }
                }

                val date_stamp = starts[0].toDate() // Timestamp를 Date로 변환
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // 날짜 형식 지정
                val date = dateFormat.format(date_stamp)

                val visit = Visitlist(logID, courseName, tags, places, time, date)
                visitList.add(visit)
            }

            // 어댑터를 초기화하고 데이터를 설정
            adapter = VisitAdapter(visitList, this@VisitFragment)
            recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
            Log.d("VisitFragment", "Fetched data: $visitList")
        } catch (e: Exception) {
            // Handle exceptions appropriately
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
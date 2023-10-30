package com.example.trekbuddy.ui.guide

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.trekbuddy.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.media.MediaPlayer
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import com.example.trekbuddy.databinding.FragmentInformBinding

class InformFragment : Fragment() {
    private lateinit var binding: FragmentInformBinding
    private val storage = FirebaseStorage.getInstance()
    private var mediaPlayer: MediaPlayer? = null
    private var playGuideButton: ImageButton? = null
    private var pauseGuideButton: ImageButton? = null
    var course_name:String="temp_course"
    var place_name:String="temp_place"

    private fun loadGuideDataFromFirestoreAndStorage() {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("CourseList").document("course1").collection("PlaceList").document("place2")

        docRef
            .get()
            .addOnSuccessListener { document ->
                val guideVoiceURL = document.getString("guide_voice")
                if (guideVoiceURL != null) {
                    downloadDataFromStorage(guideVoiceURL) { guideVoiceData ->
                        if (guideVoiceData != null) {
                            playGuideVoice(guideVoiceData)
                        } else {
                            Log.d("voice", "no voice3, doc_id: $place_name")
                        }
                    }

                } else {
                    Log.d("voice", "no voice3, doc_id: $place_name")
                }

            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error loading Firestore data: $exception")
            }
    }



    private fun downloadDataFromStorage(url: String, callback: (ByteArray?) -> Unit) {
        val storageRef = storage.getReferenceFromUrl(url)

        storageRef.getBytes((1 * 1024 * 1024 * 1.44).toLong()) // 최대 바이트 수를 설정하거나 필요에 따라 수정
            .addOnSuccessListener { data ->
                callback(data)
                Log.d("voice", "yes voice ")
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseStorageError", "Error downloading data: $exception")
                callback(null)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 바인딩을 초기화합니다
        binding = FragmentInformBinding.inflate(inflater, container, false)
        val root = binding.root

        // 바인딩을 사용하여 뷰에 접근합니다
        val destination = this.arguments?.getString("markerTitle")
        println(destination) // 출력 확인

        // 루트 뷰를 반환합니다
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toBackButton = view.findViewById<Button>(R.id.Toback)
        toBackButton.setOnClickListener {
            // GuideFragment로 이동하는 코드를 추가합니다.
            val guideFragment = GuideFragment() // 이동할 Fragment를 초기화합니다.

            // FragmentManager를 사용하여 트랜잭션을 시작합니다.
            parentFragmentManager.beginTransaction().apply {
                replace(R.id.fragment_container, guideFragment) // R.id.fragment_container는 Fragment를 표시할 레이아웃의 ID입니다.
                addToBackStack(null) // Fragment를 백 스택에 추가합니다. (선택 사항)
                commit() // 트랜잭션을 커밋하여 Fragment 전환을 완료합니다.
            }
        }
         //뒤로가기 버튼 구현(10-27)

        val playGuideButton = view.findViewById<ImageButton>(R.id.playGuideButton)
        playGuideButton?.setOnClickListener {
            onPlayGuideButtonClick(it)
        }

        val pauseGuideButton = view.findViewById<ImageButton>(R.id.pauseGuideButton)
        pauseGuideButton?.setOnClickListener {
            onPauseGuideButtonClick(it)
        }

        //스크립트는 버튼 안 눌러도 바로 화면으로 넘어오는 즉시 보이도록 하는 부분
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("CourseList").document("course1").collection("PlaceList").document("place2")

        docRef
            .get()
            .addOnSuccessListener { document ->
                val guideScriptURL = document.getString("guide_script")
                if (guideScriptURL != null) {
                    loadGuideScript(guideScriptURL)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error loading Firestore data: $exception")
            }
    }

    private fun playGuideVoice(voiceData: ByteArray?) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        } else {
            // 재생 중인 경우, 중지하고 초기화
            mediaPlayer?.stop()
            mediaPlayer?.reset()
        }

        try {
            // 바이트 배열에서 오디오 데이터를 재생할 수 있도록 설정
            val tempFile = File.createTempFile("guideVoice", "mp3", context?.cacheDir)
            FileOutputStream(tempFile).use {
                it.write(voiceData)
            }
            val dataSource = tempFile.absolutePath

            // 미디어 플레이어에 데이터 소스 설정
            mediaPlayer?.setDataSource(dataSource)

            // 비동기적으로 데이터 소스를 준비하고 재생
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener {
                it.start()
            }
        } catch (e: Exception) {
            Log.e("MediaPlayerError", "Error playing guide voice: $e")
        }
    }

    private fun loadGuideScript(scriptURL: String) {
        val scriptTextView = view?.findViewById<TextView>(R.id.scriptTextView)
        // Initialize Firebase Storage
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReferenceFromUrl(scriptURL)

        // Download the script content from Firebase Storage
        storageRef.getBytes(3000)
            .addOnSuccessListener { scriptData ->
                val scriptContent = String(scriptData, Charsets.UTF_8)
                println(scriptContent)

                // Update the scriptTextView with the downloaded script content
                scriptTextView?.text = scriptContent
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseStorageError", "Error downloading script: $exception")
                // Handle the error as needed
            }
    }

    fun onPlayGuideButtonClick(view: View) {
        playGuideButton?.visibility = View.INVISIBLE
        pauseGuideButton?.visibility = View.VISIBLE

        if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
            // 음성이 이미 재생 중인 경우 중지
            mediaPlayer?.pause()
        } else {
            // 음성이 재생 중이 아닌 경우 음성 재생 시작
            loadGuideDataFromFirestoreAndStorage()
        }
    }

    fun onPauseGuideButtonClick(view: View) {
        playGuideButton?.visibility = View.VISIBLE
        pauseGuideButton?.visibility = View.INVISIBLE

        if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
            // 음성이 재생 중이면 일시 정지
            mediaPlayer?.pause()
        } else {
            // 음성이 재생 중이 아닌 경우 음성 재생 시작
            loadGuideDataFromFirestoreAndStorage()
        }
    }

}
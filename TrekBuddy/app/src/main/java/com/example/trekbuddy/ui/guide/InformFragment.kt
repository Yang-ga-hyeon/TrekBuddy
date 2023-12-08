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
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import com.example.trekbuddy.databinding.FragmentInformBinding
import com.example.trekbuddy.ui.profile.ProfileFragment

class InformFragment : Fragment() {
    private lateinit var binding: FragmentInformBinding
    private val storage = FirebaseStorage.getInstance()
    private var mediaPlayer: MediaPlayer? = null
    private var playGuideButton: ImageButton? = null
    private var pauseGuideButton: ImageButton? = null
    private var isPlaying: Boolean = false
    private var playbackPosition: Int = 0

    private fun loadDataFromFirestore(markerTitle: String) {
        val db = FirebaseFirestore.getInstance()
        val placeListRef = db.collection("PlaceList")

        placeListRef
            .whereEqualTo("name", markerTitle) // name 필드와 markerTitle이 일치하는 문서를 찾습니다.
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // 매치되는 문서를 찾은 경우 해당 문서에서 데이터를 가져와 UI를 업데이트합니다.
                    val placeName = document.getString("name")
                    val scriptUrl = document.getString("script")
                    val voiceUrl = document.getString("voice")
                    val imageUrl = document.getString("image")

                    // UI 업데이트 코드 추가
                    updateUIWithPlaceData(placeName, scriptUrl, voiceUrl, imageUrl)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error getting documents: $exception")
            }
    }

    private fun updateUIWithPlaceData(placeName: String?,  scriptUrl: String?, voiceUrl: String?, imageUrl: String?) {
        // UI를 업데이트합니다. 예를 들어, TextView에 데이터를 설정할 수 있습니다.
        val nameTextView = binding.textView
        val scriptTextView = binding.scriptTextView
        val imageView = binding.imageView

        nameTextView.text = placeName
        scriptTextView.text = scriptUrl

        // 스크립트 불러오기
        if (scriptUrl != null) {
            loadGuideScript(scriptUrl)
        }

        // 이미지를 가져와서 imageView에 설정
        if (imageUrl != null) {
            // Glide나 Picasso 등의 이미지 로딩 라이브러리를 사용하여 이미지를 가져와 imageView에 설정합니다.
            Glide.with(this)
                .load(imageUrl)
                .into(imageView) // imageView는 이미지를 표시할 ImageView입니다.
        }
// 음성 재생
        if (voiceUrl != null) {
            mediaPlayer = MediaPlayer()
            try {
                mediaPlayer?.setDataSource(voiceUrl)
                mediaPlayer?.prepareAsync()
                mediaPlayer?.setOnPreparedListener {
                    it.start()
                    isPlaying = true

                    val seekBar = view?.findViewById<SeekBar>(R.id.seekBar)
                    seekBar?.max = mediaPlayer?.duration ?: 0

                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed(object : Runnable {
                        override fun run() {
                            if (mediaPlayer != null && isPlaying) {
                                val currentPosition = mediaPlayer?.currentPosition ?: 0
                                seekBar?.progress = currentPosition
                                handler.postDelayed(this, 100)
                            }
                        }
                    }, 0)
                }
            } catch (e: Exception) {
                Log.e("MediaPlayerError", "Error playing guide voice: $e")
            }
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

        // backButton의 클릭 이벤트 리스너
        val backButton = root.findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.navigation_guide)
        }

        // 루트 뷰를 반환합니다
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markerTitle = arguments?.getString("markerTitle")
        playGuideButton = view.findViewById(R.id.playGuideButton)
        pauseGuideButton = view.findViewById(R.id.pauseGuideButton)

        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 사용자가 seekBar를 조작할 때 호출됩니다.
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 사용자가 seekBar를 터치할 때 호출됩니다.
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 사용자가 seekBar 터치를 끝낼 때 호출됩니다.
                seekBar?.let{ val newPosition = seekBar.progress
                    mediaPlayer?.seekTo(newPosition)}
            }
        })

        val forwardGuideButton = view.findViewById<ImageButton>(R.id.forwardGuideButton)
        forwardGuideButton?.setOnClickListener {
            onForwardGuideButtonClick(it)
        }
        val backwardGuideButton = view.findViewById<ImageButton>(R.id.backwardGuideButton)
        backwardGuideButton?.setOnClickListener {
            onBackwardGuideButtonClick(it)
        }
        if (markerTitle != null) {
            loadDataFromFirestore(markerTitle)
        }

        val playGuideButton = view.findViewById<ImageButton>(R.id.playGuideButton)
        playGuideButton?.setOnClickListener {
            onPlayGuideButtonClick(it)
        }

        val pauseGuideButton = view.findViewById<ImageButton>(R.id.pauseGuideButton)
        pauseGuideButton?.setOnClickListener {
            onPauseGuideButtonClick(it)
        }

        // 스크립트 불러오는 부분
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


    fun playGuideVoice(voiceUrl: String?, position: Int) {
        if (voiceUrl != null) {
            mediaPlayer?.reset()
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(voiceUrl)
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener {
                mediaPlayer?.seekTo(position)
                mediaPlayer?.start()
                isPlaying = true
            }
        }
        else {
            mediaPlayer?.seekTo(position)
            mediaPlayer?.start()
            isPlaying = true
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

        if (mediaPlayer != null) {
            val voiceUrl = this.arguments?.getString("voiceUrl")
            playGuideVoice(voiceUrl, playbackPosition)
        }
    }

    fun onPauseGuideButtonClick(view: View) {
        playGuideButton?.visibility = View.VISIBLE
        pauseGuideButton?.visibility = View.INVISIBLE

        if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            playbackPosition = mediaPlayer?.currentPosition ?: 0
        }
    }

    fun onForwardGuideButtonClick(view: View) {
        val currentPosition = mediaPlayer?.currentPosition ?: 0
        val duration = mediaPlayer?.duration ?: 0

        if (currentPosition + 10000 < duration) {
            mediaPlayer?.seekTo(currentPosition + 10000)
        } else {
            mediaPlayer?.seekTo(duration)
        }
    }

    fun onBackwardGuideButtonClick(view: View) {
        val currentPosition = mediaPlayer?.currentPosition ?: 0

        if (currentPosition - 10000 > 0) {
            mediaPlayer?.seekTo(currentPosition - 10000)
        } else {
            mediaPlayer?.seekTo(0)
        }
    }


    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}
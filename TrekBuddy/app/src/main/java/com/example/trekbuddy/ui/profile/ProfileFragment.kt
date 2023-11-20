package com.example.trekbuddy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.fragment.findNavController
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentProfileBinding
import com.example.trekbuddy.ui.guide.InformFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    val auth = FirebaseAuth.getInstance()
    val currentUser: FirebaseUser? = auth.currentUser
    private val db = FirebaseFirestore.getInstance()

    val uid = currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val visitButton = binding.visitButton
        val likeButton = binding.likeButton
        val nicknameView = binding.nicknameView
        val emailView = binding.textView2
        val editNickButton = binding.editNickButton

        emailView.text = currentUser?.email

        val nicknameRef = db.collection("Users").document(uid.toString())
        nicknameRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // document가 존재하면 nickname 필드 값을 변수에 저장
                    val nickname = documentSnapshot.getString("nickname")
                    nicknameView.text = nickname

                } else {
                    println("해당 사용자를 찾을 수 없음.")
                }
            }
            .addOnFailureListener { e ->
                println("문서 가져오기 오류: $e")
            }

        visitButton.setOnClickListener {

            val visitFragment = VisitFragment()
            val args = Bundle()
            args.putString("currentUserId", currentUser?.email) // 여기서 "currentUserId"는 사용자 ID를 저장할 키입니다.
            visitFragment.arguments = args

            findNavController().navigate(R.id.action_navigation_profile_to_navigation_visit, args)

        }

        likeButton.setOnClickListener{
            findNavController().navigate(R.id.action_navigation_profile_to_navigation_like)
        }

        editNickButton.setOnClickListener {

            val args = Bundle()
            args.putString("currentUserId", currentUser?.email) // 사용자 ID를 전달

            findNavController().navigate(
                R.id.action_navigation_profile_to_navigation_edit, args)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
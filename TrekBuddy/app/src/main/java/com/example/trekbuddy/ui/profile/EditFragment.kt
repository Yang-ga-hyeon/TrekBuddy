package com.example.trekbuddy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentEditBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!
    private var currentUserId: String? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        currentUserId = arguments?.getString("currentUserId")

        val backButton = root.findViewById<ImageView>(R.id.backButton)
        // backButton의 클릭 이벤트 리스너
        backButton.setOnClickListener {// ProfileFragment로 이동
            findNavController().navigateUp()
        }

        val saveButton = root.findViewById<AppCompatButton>(R.id.saveButton)
        saveButton.setOnClickListener {
            println("저장 버튼 클릭")
            // 사용자가 입력한 닉네임 가져오기
            val newNickname = binding.editNicknameField.text.toString()

            // Firestore에 업데이트할 데이터 준비
            val userData = hashMapOf(
                "nickname" to newNickname
            )

            currentUserId?.let { userId ->
                // "Users" 컬렉션에서 "email" 필드가 currentUserId와 일치하는 문서 조회
                val userRef = db.collection("Users")
                    .whereEqualTo("email", userId)

                userRef.get().addOnSuccessListener { documents ->
                    for (document in documents) {
                        // 찾은 문서의 "nickname" 필드 업데이트
                        val documentReference = db.collection("Users").document(document.id)
                        documentReference.update("nickname", newNickname)
                            .addOnSuccessListener {
                                println("닉네임 업데이트 성공")
                                Toast.makeText(requireContext(), "닉네임이 변경되었습니다.", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener {
                                // 업데이트 실패
                            }
                    }
                }
            }

        }


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
package com.example.trekbuddy

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val emailEditText = findViewById<EditText>(R.id.EmailAddress)
        val passwordEditText = findViewById<EditText>(R.id.passwordInput)
        val nicknameEditText = findViewById<EditText>(R.id.nicknameInput)
        val ageEditText = findViewById<EditText>(R.id.ageInput)
        val kakaoIDEditText = findViewById<EditText>(R.id.kakaoIDInput)
        val genderRadioGroup = findViewById<RadioGroup>(R.id.genderRadioGroup)

        val signUpButton = findViewById<Button>(R.id.button)
        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val nickname = nicknameEditText.text.toString()
            val age = ageEditText.text.toString()
            val kakaoID = kakaoIDEditText.text.toString()
            val selectedGender = if (genderRadioGroup.checkedRadioButtonId == R.id.radioButtonMale) "Male" else "Female"

            if (email.isNotEmpty() && password.isNotEmpty() && nickname.isNotEmpty() && age.isNotEmpty() && kakaoID.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                val currentUserDb = database.child("Users").child(userId)
                                currentUserDb.child("email").setValue(email)
                                currentUserDb.child("nickname").setValue(nickname)
                                currentUserDb.child("age").setValue(age)
                                currentUserDb.child("kakaoId").setValue(kakaoID)
                                currentUserDb.child("gender").setValue(selectedGender)
                            }
                            val intent: Intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            if (task.exception is FirebaseAuthUserCollisionException) {
                                // 이미 존재하는 이메일 주소에 대한 오류 처리
                                Toast.makeText(this, "이미 존재하는 이메일 주소입니다", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
                                Log.d("SignUp", "Error: ${task.exception}")
                            }
                        }
                    }
            } else {
                // 필수 정보가 모두 입력되지 않았을 때 처리할 부분을 추가하십시오.
                Toast.makeText(this, "모든 필수 정보를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

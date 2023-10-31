package com.example.trekbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.trekbuddy.NavigationActivity
import com.example.trekbuddy.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.EmailAddress)
        val passwordEditText = findViewById<EditText>(R.id.Password)

        val locationTextView = findViewById<TextView>(R.id.locationTextView)

        val loginButton = findViewById<Button>(R.id.login)
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // 로그인 성공 시 NavigationActivity로 이동
                            val intent = Intent(this, NavigationActivity::class.java)
                            startActivity(intent)
                            finish() // 현재 액티비티 종료
                        } else {
                            // 로그인 실패 시 동작
                            Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "이메일 및 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        val signUpButton = findViewById<Button>(R.id.signup)
        signUpButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        checkLocationPermission()
    }
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLocation()
        }
    }
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )
                    if (addresses != null && addresses.isNotEmpty()) {
                        val country = addresses[0].countryName
                        val locationText = "$country"
                        findViewById<TextView>(R.id.locationTextView).text = locationText
                    } else {
                        findViewById<TextView>(R.id.locationTextView).text = "주소를 찾을 수 없습니다."
                    }
                }
            }
            .addOnFailureListener { e ->
                findViewById<TextView>(R.id.locationTextView).text = "위치를 가져올 수 없습니다: ${e.message}"
            }
    }
}

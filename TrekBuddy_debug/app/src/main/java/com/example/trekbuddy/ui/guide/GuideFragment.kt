package com.example.trekbuddy.ui.guide

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentGuideBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class GuideFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener, LocationDataListener, GoogleMap.OnMarkerClickListener {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private var _binding: FragmentGuideBinding? = null
    private val binding get() = _binding!!
    private var markerButtonVisible = false // 추가

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CourseAdapter
    private var googleMap: GoogleMap? = null
    private val db = FirebaseFirestore.getInstance()
    var userLocation = Location("UserLocation")

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var permissionDenied = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideBinding.inflate(inflater, container, false)
        val root: View = binding.root
        recyclerView = root.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Firebase에서 데이터를 가져와서 Adapter에 전달
        val coursesList = ArrayList<Course>() // Firebase에서 가져온 데이터

        // Firestore에서 데이터를 가져와 RecyclerView에 표시
        db.collection("CourseList")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val coursesList = ArrayList<Course>()
                for (document in querySnapshot) {
                    val courseName = document["name"].toString()
                    val imageUrl = document["image"].toString()
                    val latitude = document["latitude"].toString()
                    val longitude = document["longitude"].toString()
                    val locationName = document["name"].toString()

                    if (courseName != null && imageUrl != null) {
                        val course =
                            Course(null, courseName, imageUrl, latitude, longitude, locationName)
                        coursesList.add(course)
                    }
                }
                adapter = CourseAdapter(coursesList, googleMap, this)
                recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // 오류 처리
            }

        return root
    }

    private val callback = OnMapReadyCallback { googleMap ->

        this.googleMap = googleMap
        // 파이어베이스 컬렉션 참조
        val collectionRef = db.collection("CourseList")

        // 데이터 가져오기
        collectionRef.get()
            .addOnSuccessListener { documents ->
                val locations = ArrayList<LatLng>() // LatLng의 배열로 장소의 위치를 저장

                for (document in documents) {
                    // Firestore 문서에서 위도와 경도 필드 가져오기
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    val locationName = document.getString("name")

                    if (latitude != null && longitude != null && locationName != null) {
                        val location = LatLng(latitude, longitude)
                        googleMap.addMarker(MarkerOptions().position(location).title(locationName))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
                        locations.add(location)
                    }
                }

            }
            .addOnFailureListener { exception ->
                // 오류 처리
                Log.e("FirebaseError", "Error getting documents: ${exception.message}")
            }
        googleMap.setOnMarkerClickListener(this)
    }

    override fun onLocationDataLoaded(locationArrayList: List<LatLng>, locationNames: List<String>)  {
        // 파란색 마커 아이콘 설정
        val blueMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)

        for (i in 0 until locationArrayList.size) {
            // 목록의 각 위치에 마커를 추가
            googleMap?.addMarker(MarkerOptions().position(locationArrayList[i]).title(locationNames[i]).icon(blueMarkerIcon))
            googleMap?.animateCamera(CameraUpdateFactory.zoomTo(18.0f))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLng(locationArrayList[i]))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        enableMyLocation()
        getUserLocation()

        val collectionRef = db.collection("CourseList")

        collectionRef.get()
            .addOnSuccessListener { documents ->
                val locations = ArrayList<LatLng>()

                for (document in documents) {
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    val locationName = document.getString("name")

                    if (latitude != null && longitude != null && locationName != null) {
                        val location = LatLng(latitude, longitude)

                        googleMap.addMarker(MarkerOptions().position(location).title(locationName))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
                        locations.add(location)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Error getting documents: ${exception.message}")
            }
        googleMap.setOnMarkerClickListener(this)
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    // 사용자의 현재 위치를 userLocation에 설정
                    userLocation = it
                }
            }
            .addOnFailureListener { e ->
                // 위치 정보를 가져오는 데 실패한 경우 처리
                Log.e("LocationError", "Failed to get location: ${e.message}")
            }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            return
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            // Show rationale and request permission.
        } else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(requireContext(), "현재위치", Toast.LENGTH_SHORT)
            .show()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(requireContext(), "현재위치:\n$location", Toast.LENGTH_LONG)
            .show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (isPermissionGranted(
                    permissions,
                    grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) || isPermissionGranted(
                    permissions,
                    grantResults,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                enableMyLocation()
            } else {
                // Permission was denied. Display an error message
                permissionDenied = true
                showMissingPermissionError()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showMissingPermissionError() {
        Toast.makeText(
            requireContext(),
            "Location permission is required for this app to function",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isPermissionGranted(permissions: Array<String>, grantResults: IntArray, permission: String): Boolean {
        for (i in permissions.indices) {
            if (permission == permissions[i]) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED
            }
        }
        return false
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val textView = view?.findViewById<TextView>(R.id.textView)
        val tourButton = view?.findViewById<Button>(R.id.TourButton)
        //마커의 위치
        val markerLocation = Location("MarkerLocation")
        markerLocation.latitude = marker.position.latitude // 마커의 실제 위도 값으로 변경
        markerLocation.longitude = marker.position.longitude // 마커의 실제 경도 값으로 변경

        println(userLocation.latitude)
        println(userLocation.longitude)
        println(markerLocation.latitude)
        println(markerLocation.longitude)
        val distance = userLocation.distanceTo(markerLocation)

        println(distance)
        tourButton?.setOnClickListener {
            var informFragment = InformFragment()
            val markerTitle = marker.title

            val bundle = Bundle()
            bundle.putString("markerTitle", markerTitle)
            informFragment.arguments = bundle
            activity?.supportFragmentManager!!.beginTransaction()
                .replace(R.id.container, informFragment)
                .commit()
        }

        tourButton?.setOnClickListener {
            var informFragment = InformFragment()
            val markerTitle = marker.title
            val bundle = Bundle()
            bundle.putString("markerTitle", markerTitle)
            informFragment.arguments = bundle
            activity?.supportFragmentManager!!.beginTransaction()
                .replace(R.id.container, informFragment)
                .commit()
        }

        //파란색 마커일 경우
        if (marker.title != "이화여자대학교" && marker.title != "경복궁"){
            if (textView != null && tourButton != null) {
                textView.text = "\t 투어할 장소를 선택해주세요: "+marker.title
                if (!markerButtonVisible) {
                    tourButton.visibility = View.VISIBLE
                    markerButtonVisible = true
                    if (distance <= 50) {
                        tourButton.isEnabled = true
                        tourButton.alpha = 1.0f // 버튼을 완전히 불투명하지 않게 만듭니다.
                    } else {
                        tourButton.isEnabled = false
                        tourButton.alpha = 0.5f // 버튼을 불투명하게 만듭니다.
                    }

                }
                else{
                    if (distance <= 50) {
                        tourButton.isEnabled = true
                        tourButton.alpha = 1.0f // 버튼을 완전히 불투명하지 않게 만듭니다.
                    } else {
                        tourButton.isEnabled = false
                        tourButton.alpha = 0.5f // 버튼을 불투명하게 만듭니다.
                    }
                }
            }
        }
        else{
            if (textView != null && tourButton != null) {
                textView.text = "투어할 장소를 선택해주세요: "
                tourButton.visibility = View.INVISIBLE
                markerButtonVisible = false
            }

        }
        return false
    }
}

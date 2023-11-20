package com.example.trekbuddy.ui.guide

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentGuideBinding
import com.example.trekbuddy.ui.SearchCourse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class GuideFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener, LocationDataListener, GoogleMap.OnMarkerClickListener {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }
    private var coursesList = ArrayList<Course>()

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
    private var searchCourse: SearchCourse? = null
    private var userID: String? = null
    private lateinit var searchResultLayout: View
    private lateinit var GoBackButton: ImageView
    private lateinit var likeYesButton: ImageView
    private lateinit var likeNoButton: ImageView

    private lateinit var searchResultNameView: TextView
    private lateinit var searchResultTimeView: TextView
    private lateinit var tagTextView: TextView
    private lateinit var placesTextView: TextView

    private val logCollection = FirebaseFirestore.getInstance().collection("Log")


    // Search -> Guide
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Arguments에서 데이터 추출
        arguments?.let {
            searchCourse = it.getParcelable("SearchCourse")
            userID = it.getString("currentUserId")
        }
        // searchCourse 및 userID를 사용하여 원하는 작업을 수행
        searchCourse?.let { course ->
            val places = course.places
            val placeListRef = db.collection("PlaceList")
            val positions = ArrayList<LatLng>()
            val titles = ArrayList<String>()

            val placeQueries = places.map { place ->
                placeListRef.whereEqualTo("name", place)
            }

            // CoroutineScope를 사용하여 비동기 작업을 처리
            CoroutineScope(Dispatchers.Main).launch {
                val deferredResults = placeQueries.map { placeQuery ->
                    async(Dispatchers.IO) {
                        placeQuery.get().await()
                    }
                }

                val placeDocumentsList = deferredResults.awaitAll()

                for (placeDocuments in placeDocumentsList) {
                    for (document in placeDocuments) {
                        val title = document.getString("name") ?: ""
                        val geoPoint = document.getGeoPoint("coordinate")

                        if (geoPoint != null) {
                            val location = LatLng(geoPoint.latitude, geoPoint.longitude)
                            positions.add(location)
                        }

                        titles.add(title)
                    }
                }

                onLocationDataLoaded(positions, titles)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideBinding.inflate(inflater, container, false)
        val root: View = binding.root
        recyclerView = root.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchResultLayout = root.findViewById(R.id.search_result)
        searchResultNameView = root.findViewById(R.id.courseNameTextView)
        searchResultTimeView = root.findViewById(R.id.timeTextView)
        GoBackButton = root.findViewById(R.id.GoBackButtonToMatching)
        tagTextView = root.findViewById(R.id.tagTextView)
        likeYesButton =  root.findViewById(R.id.likeYesButton)
        likeNoButton =  root.findViewById(R.id.likeNoButton)
        placesTextView = root.findViewById(R.id.placesTextView)

        GoBackButton.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.navigation_guide)
        }

        // 검색결과 표시
        if (searchCourse!=null){
            recyclerView.visibility = View.GONE
            searchResultLayout.visibility = View.VISIBLE
            searchResultNameView.text = searchCourse!!.name.toString()
            tagTextView.text = searchCourse!!.tags.joinToString(prefix = " #", separator = " #")
            placesTextView.text = searchCourse!!.places.joinToString(" -> ")

            val time = searchCourse!!.time.toInt()
            if (time >= 60) {
                val hour = time / 60
                val min = time % 60
                searchResultTimeView.text = hour.toString() + "시간" + min.toString() + "분"
            } else {
                searchResultTimeView.text = time.toString() + "분"
            }
            if (searchCourse!!.isLiked) {
                likeYesButton.visibility = View.VISIBLE
                likeNoButton.visibility = View.INVISIBLE
                likeYesButton.setOnClickListener {
                }
            } else {
                likeYesButton.visibility = View.INVISIBLE
                likeNoButton.visibility = View.VISIBLE
                likeNoButton.setOnClickListener {
                }
            }
        }
        else{
            recyclerView.visibility = View.VISIBLE
            searchResultLayout.visibility = View.GONE
        }

        // Firestore에서 데이터를 가져와 RecyclerView에 표시
        db.collection("SystemCourseList")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val coursesList = ArrayList<Course>()
                for (document in querySnapshot) {
                    val courseName = document["name"].toString()
                    val imageUrl = document["image"].toString()
                    val latitude = document["latitude"].toString()
                    val longitude = document["longitude"].toString()
                    val locationName = document["name"].toString()

                    if (courseName != null && imageUrl != null&& latitude != null && longitude != null && locationName != null) {
                        val course =
                            Course(null, courseName, imageUrl, latitude, longitude, locationName)
                        coursesList.add(course)
                    }
                }
                val filteredCourses = filterCoursesByDistance(LatLng(userLocation.latitude, userLocation.longitude), coursesList)
                adapter = CourseAdapter(filteredCourses, googleMap, this)
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
        val collectionRef = db.collection("SystemCourseList")

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
        val blueMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)

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

        val zoomLevel = 12.0f // 원하는 줌 레벨로 설정 (조정 가능)
        val userLatLng = LatLng(userLocation.latitude, userLocation.longitude) // 사용자의 위치 가져오기

        val circleOptions = CircleOptions()
            .center(userLatLng)
            .radius(5000.0) // 5km를 미터로 변환하여 설정
            .strokeWidth(2f)

        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(userLatLng, zoomLevel) // 사용자의 위치로 카메라 이동
        googleMap.moveCamera(cameraUpdate) // 구글 맵에 적용

        val collectionRef = db.collection("SystemCourseList")

        collectionRef.get()
            .addOnSuccessListener { documents ->
                val locations = ArrayList<LatLng>()

                for (document in documents) {
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    val locationName = document.getString("name")

                    if (latitude != null && longitude != null && locationName != null) {
                        val location = LatLng(latitude, longitude)
                        val filteredCourses = filterCoursesByDistance(
                            LatLng(userLocation.latitude, userLocation.longitude),
                            coursesList
                        )
                        val distance = getDistanceInKm(
                            LatLng(userLocation.latitude, userLocation.longitude),
                            location)
                        if (distance <= 5 && searchCourse==null) {
                            googleMap.addMarker(MarkerOptions().position(location).title(locationName))
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(location))
                            locations.add(location)
                        }
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
        /*fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    // 사용자의 현재 위치를 userLocation에 설정
                    userLocation = it
                }
            }
            .addOnFailureListener { e ->
                // 위치 정보를 가져오는 데 실패한 경우 처리
                Log.e("LocationError", "Failed to get location: ${e.message}")
            }*/
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 위치 업데이트 간의 간격 (밀리초)
            fastestInterval = 5000 // 가장 빠른 위치 업데이트 간의 간격 (밀리초)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // 정확도 우선
        }
        // 위치 업데이트 요청
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )

        // 마지막으로 알려진 위치 가져오기
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

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                // 새로운 위치 업데이트가 있을 때 호출되는 콜백
                userLocation = it
            }
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
        //todo: 여기는 되는데 왜 안되냐고
        Toast.makeText(requireContext(), "현재위치", Toast.LENGTH_SHORT)
        val zoomLevel = 15.0f // 원하는 줌 레벨로 설정 (조정 가능)
        val userLatLng = LatLng(userLocation.latitude, userLocation.longitude) // 사용자의 위치 가져오기

        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(userLatLng, zoomLevel) // 사용자의 위치로 카메라 이동
        googleMap?.moveCamera(cameraUpdate) // 구글 맵에 적용

        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(requireContext(), "현재위치:\n$location", Toast.LENGTH_LONG)
            .show()
        println("dot"+location.latitude)
        println("dot"+location.longitude)
        println("dot"+userLocation.latitude)
        println("dot"+userLocation.longitude)
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

    private fun filterCoursesByDistance(currentLocation: LatLng, coursesList: List<Course>): List<Course> {
        val filteredCourses = mutableListOf<Course>()
        for (course in coursesList) {
            val courseLocation = LatLng(course.latitude.toDouble(), course.longitude.toDouble())
            val distance = getDistanceInKm(currentLocation, courseLocation)
            if (distance <= 5) {
                filteredCourses.add(course)
            }
        }
        return filteredCourses
    }

    // 두 지점 간의 거리 계산
    private fun getDistanceInKm(location1: LatLng, location2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude, results
        )
        return results[0] / 1000 // m to km
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        //todo: 왜 안되노
        val textView = view?.findViewById<TextView>(R.id.textView)
        val tourButton = view?.findViewById<ImageView>(R.id.TourButton)

        //마커의 위치
        val markerLocation = Location("MarkerLocation")
        markerLocation.latitude = marker.position.latitude // 마커의 실제 위도 값으로 변경
        markerLocation.longitude = marker.position.longitude // 마커의 실제 경도 값으로 변경

        println("marker"+userLocation.latitude)
        println("marker"+userLocation.longitude)
        val distance = userLocation.distanceTo(markerLocation)

        tourButton?.setOnClickListener {
            val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                val userEmail = currentUser.email
                val placelistCollection = db.collection("PlaceList")
                val logCollection = FirebaseFirestore.getInstance().collection("Log")

                // 클릭한 관광지의 정보 가져오기
                val clickedPlace = marker.title
                val clickedTime = com.google.firebase.Timestamp.now()

                placelistCollection
                    .whereEqualTo("name", clickedPlace)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents != null && !documents.isEmpty) {
                            val placelistDocumentId = documents.documents[0].id

                            // 기존에 해당 사용자 ID로 저장된 문서가 있는지 확인
                            logCollection
                                .whereEqualTo("userID", userEmail)
                                .whereEqualTo("timeflag", true)
                                .get()
                                .addOnSuccessListener { logDocuments ->
                                    if (logDocuments != null && !logDocuments.isEmpty) {
                                        // 이미 해당 사용자 ID로 저장된 문서가 있으면 기존 문서에 데이터 추가
                                        for (logDocument in logDocuments.documents) {
                                            // 자정을 지났는지 확인
                                            if (isPastMidnight(logDocument, clickedTime)) {
                                                // 자정이 지났을 때, 기존 문서의 timeflag를 false로 변경
                                                logDocument.reference.update("timeflag", false)

                                                // 새로운 문서를 만들도록 수정
                                                createNewLogDocument(placelistDocumentId, clickedTime, userEmail!!)
                                            } else {
                                                // 자정을 지나지 않았을 때, 기존 문서에 데이터 추가
                                                logDocument.reference.update(
                                                    "places", FieldValue.arrayUnion(placelistDocumentId),
                                                    "starts", FieldValue.arrayUnion(clickedTime)
                                                )
                                            }
                                        }
                                    } else {
                                        // 해당 사용자 ID로 저장된 문서가 없으면 새로운 문서를 만들도록 수정
                                        createNewLogDocument(placelistDocumentId, clickedTime, userEmail!!)
                                    }
                                }
                        } else {
                            // 관광지 정보를 찾지 못한 경우에 대한 처리
                        }
                    }
            }

            val markerTitle = marker.title
            val bundle = Bundle()
            bundle.putString("markerTitle", markerTitle)
            findNavController().navigate(
                R.id.action_navigation_guide_to_navigation_inform,
                bundle

            )
        }


        //파란색 마커일 경우
        if (marker.title != "이화여자대학교" && marker.title != "경복궁"){
            if (textView != null && tourButton != null) {
                textView.text = "\t 투어할 장소를 선택해주세요: "+marker.title
                if (!markerButtonVisible) {
                    tourButton.visibility = View.VISIBLE
                    markerButtonVisible = true
                    if (distance <= 100) {
                        tourButton.isEnabled = true
                        tourButton.alpha = 1.0f // 버튼을 완전히 불투명하지 않게 만듭니다.
                    } else {
                        tourButton.isEnabled = false
                        tourButton.alpha = 0.5f // 버튼을 불투명하게 만듭니다.
                    }

                }
                else{
                    if (distance <= 100) {
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

    private fun createNewLogDocument(
        placelistDocumentId: String,
        clickedTime: com.google.firebase.Timestamp,
        userEmail: String
    ) {
        val newLogDocument = logCollection.document()

        // 필드 설정
        val logData = hashMapOf(
            "logname" to "",
            "places" to arrayListOf(placelistDocumentId),
            "starts" to arrayListOf(clickedTime),
            "timeflag" to true,
            "userID" to userEmail
        )

        // 문서에 필드 설정
        newLogDocument.set(logData)
            .addOnSuccessListener {
                // 성공적으로 문서가 추가됨
                Log.d("Firestore", "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                // 오류 처리
                Log.w("Firestore", "Error writing document", e)
            }
    }

    private fun isPastMidnight(document: DocumentSnapshot, clickedTime: com.google.firebase.Timestamp): Boolean {
        val currentCalendar = Calendar.getInstance()
        // 문서에서 'starts' 필드 가져오기
        val startsArray = document["starts"] as? List<com.google.firebase.Timestamp>
        // 'starts' 배열이 비어있거나 null인 경우 false 반환
        if (startsArray.isNullOrEmpty()) {
            return false
        }

        // 'starts' 배열에서 첫 번째 배열 요소의 날짜 정보 가져오기
        val firstTimestampDate = startsArray[0].toDate()

        // clickedTime의 날짜 정보 가져오기
        val clickedTimeDate = clickedTime.toDate()

        val nextDayCalendar = Calendar.getInstance().apply {
            time = firstTimestampDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
            return clickedTimeDate.after(nextDayCalendar.time)
    }


    companion object {
        fun newInstance(searchCourse: SearchCourse, currentUserId: String?): GuideFragment {
            val fragment = GuideFragment()
            val args = Bundle()
            args.putParcelable("SearchCourse", searchCourse)
            args.putString("currentUserId", currentUserId)
            fragment.arguments = args
            return fragment
        }
    }
}

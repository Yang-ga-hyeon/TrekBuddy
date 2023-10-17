package com.example.trekbuddy.ui.guide

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentGuideBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class GuideFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener {

    private var _binding: FragmentGuideBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CourseAdapter
    private var googleMap: GoogleMap? = null
    private val db = FirebaseFirestore.getInstance()

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
                adapter = CourseAdapter(coursesList, googleMap)
                recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // 오류 처리
            }

        return root
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
    }


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
        Toast.makeText(requireContext(), "MyLocation button clicked", Toast.LENGTH_SHORT)
            .show()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(requireContext(), "Current location:\n$location", Toast.LENGTH_LONG)
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

}

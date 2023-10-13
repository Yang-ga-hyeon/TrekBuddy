package com.example.trekbuddy.ui.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.CreationExtras.Empty.map
import com.example.trekbuddy.R
import com.example.trekbuddy.databinding.FragmentGuideBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng


class GuideFragment : Fragment(), OnMapReadyCallback{

    private var _binding: FragmentGuideBinding? = null
    private var googleMap: GoogleMap? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_guide, container, false)
        _binding = FragmentGuideBinding.inflate(inflater, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // val root: View = binding.root

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onMapReady(p0: GoogleMap) {
        googleMap = map

        // Google 지도 설정 및 사용 가능할 때 호출됩니다.
        // 여기에서 원하는 지도 작업을 수행하세요.

        // 예를 들어, 특정 위치로 이동:
        val latitude = 37.7749
        val longitude = -122.4194
        val zoomLevel = 10f
        val location = LatLng(latitude, longitude)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))
    }
}
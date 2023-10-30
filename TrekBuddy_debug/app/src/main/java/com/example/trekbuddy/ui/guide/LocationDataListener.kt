package com.example.trekbuddy.ui.guide
import com.google.android.gms.maps.model.LatLng

interface LocationDataListener {
    fun onLocationDataLoaded(locationArrayList: List<LatLng>, locationNames: List<String>)
}
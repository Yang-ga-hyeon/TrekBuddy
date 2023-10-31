package com.example.trekbuddy.ui.guide

import com.google.android.gms.maps.model.LatLng

data class Place(
    val name: String,
    val location: LatLng
)
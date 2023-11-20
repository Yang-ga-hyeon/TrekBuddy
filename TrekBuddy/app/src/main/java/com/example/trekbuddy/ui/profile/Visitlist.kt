package com.example.trekbuddy.ui.profile

import java.io.Serializable

data class Visitlist(
    val logID: String?,
    val courseName: String,
    val tags: List<String>,
    val places: List<String>,
    val time: Int,
    val date: String,
) : Serializable

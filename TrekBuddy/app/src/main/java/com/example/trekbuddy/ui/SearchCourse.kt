package com.example.trekbuddy.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchCourse(
    var courseId: String?,
    val name: String,
    var places: List<String>,
    var time: String,
    var tags: List<String>,
    var isLiked: Boolean,
    val userID: String?
) : Parcelable

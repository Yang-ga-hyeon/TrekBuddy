package com.example.trekbuddy.ui.matching

data class SearchCourse(
    val courseId: String?,
    val name: String,
    val places: List<String>,
    val time: String,
    val tags: List<String>,
    var isLiked: Boolean,
    val userID: String?
)


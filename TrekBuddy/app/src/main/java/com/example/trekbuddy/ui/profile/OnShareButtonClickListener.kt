package com.example.trekbuddy.ui.profile

interface OnShareButtonClickListener {
    fun onShareButtonClicked(visitData: Visitlist)
    fun showShareDialog(courseName: String)
}
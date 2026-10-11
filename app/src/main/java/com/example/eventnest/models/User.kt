package com.example.eventnest.models

data class User(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var role: String = "user",
    var createdAt: Long = 0L
)

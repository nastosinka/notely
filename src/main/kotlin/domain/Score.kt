package com.notely.domain

import kotlinx.serialization.Serializable

@Serializable
data class Score(
    val id: String,
    val folderId: String,
    val title: String,
    val originalFileName: String,
    val storedFileName: String
)

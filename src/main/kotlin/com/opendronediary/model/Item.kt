package com.opendronediary.model

data class Item(
    val id: Int,
    val name: String,
    val description: String? = null,
    val userId: Int
)


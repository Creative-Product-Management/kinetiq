package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#10B981",
    val iconName: String = "Folder",
    val orderIndex: Int = 0
)

typealias CategoryEntity = Category

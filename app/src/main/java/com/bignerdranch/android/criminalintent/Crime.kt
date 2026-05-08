package com.bignerdranch.android.criminalintent

import java.util.Date
import java.util.UUID

data class Crime(
    val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
    var requiresPolice: Boolean = false,
    var suspect: String? = null
) {
    val photoFileName
        get() = "IMG_$id.jpg"
}

package com.example.domain.validation

object HabitValidator {
    data class Result(val titleError: String? = null, val targetError: String? = null, val floorError: String? = null) {
        val isValid: Boolean get() = titleError == null && targetError == null && floorError == null
    }

    fun validate(title: String, target: Double?, floor: Double?): Result {
        var r = Result()
        if (title.isBlank()) r = r.copy(titleError = "Title is required")
        if (target == null || target <= 0.0 || target.isNaN() || target.isInfinite()) {
            r = r.copy(targetError = "Target must be greater than 0")
        }
        if (floor != null) {
            if (floor < 0.0 || floor.isNaN() || floor.isInfinite()) r = r.copy(floorError = "Floor cannot be negative")
            else if (target != null && floor > target) r = r.copy(floorError = "Floor cannot exceed target")
        }
        return r
    }
}

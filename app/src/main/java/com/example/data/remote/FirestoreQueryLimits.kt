package com.example.data.remote

/**
 * Bounds caller-controlled Firestore page sizes before building a query.
 *
 * Firestore query limits must be positive. A hard upper bound also prevents accidental
 * unbounded reads when a caller supplies an extreme value.
 */
internal fun boundedFirestoreQueryLimit(requested: Long, maximum: Long): Long {
    require(maximum >= 1L) { "Maximum Firestore query limit must be positive." }
    return requested.coerceIn(1L, maximum)
}

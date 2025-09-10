package com.dldevalopement.adnm.home.reporter

/**
 * A data class to represent a single waste type.
 * Data classes are a special kind of class in Kotlin designed to hold data.
 * The compiler automatically generates useful functions like equals(), hashCode(),
 * toString(), and copy() based on the properties defined in the primary constructor.
 *
 * @property id The unique identifier of the waste type, usually from a database.
 * @property name The name or description of the waste type (e.g., "Plastic", "Glass").
 * @property pricePerKg The price per kilogram for this specific waste type.
 */
data class WasteType(
    val id: Int,
    val name: String,
    val pricePerKg: Double
)
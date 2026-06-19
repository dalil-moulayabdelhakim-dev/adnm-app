package com.dldevalopement.adnm.home.reporter

data class WasteItem(
    val wasteType: String,  // The type or category of the waste (e.g., plastic, metal, etc.)
    val weight: Double,     // The weight of the waste in kilograms
    val totalPrice: Double  // The total price calculated for this specific waste type
)

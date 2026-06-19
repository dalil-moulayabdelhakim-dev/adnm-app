package com.dldevalopement.adnm.home.reporter

/**
 * A data class to represent a single waste type with localized names.
 *
 * @property id The unique identifier of the waste type.
 * @property nameEn The name in English.
 * @property nameAr The name in Arabic.
 * @property nameFr The name in French.
 * @property pricePerKg The price per kilogram for this specific waste type.
 */
data class WasteType(
    val id: Int,
    val nameEn: String,
    val nameAr: String,
    val nameFr: String,
    val pricePerKg: Double
) {
    /**
     * Returns the appropriate name based on the provided language code.
     * Defaults to nameEn if no match is found.
     */
    fun getNameByLang(lang: String): String {
        return when (lang) {
            "ar" -> nameAr
            "fr" -> nameFr
            else -> nameEn
        }
    }
}

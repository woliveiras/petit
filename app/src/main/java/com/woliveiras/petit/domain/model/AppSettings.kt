package com.woliveiras.petit.domain.model

/** Available app themes. */
enum class AppTheme {
  /** Follow system setting. */
  SYSTEM,

  /** Light theme. */
  LIGHT,

  /** Dark theme. */
  DARK,
}

/** Available app languages. */
enum class AppLanguage(val code: String) {
  /** System default language. */
  SYSTEM("system"),

  /** English. */
  ENGLISH("en"),

  /** Portuguese (Brazil). */
  PORTUGUESE_BR("pt-BR");

  companion object {
    fun fromCode(code: String): AppLanguage {
      return entries.find { it.code == code } ?: SYSTEM
    }
  }
}

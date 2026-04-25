package com.woliveiras.petit.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Petit Color Palette — Amethyst Hearth
//
// Design System: docs/design-system.md
// Light theme: warm lavender with deep purple accents
// Dark theme: deep purple backgrounds with lighter purple/olive accents
// =============================================================================

// Primary — Deep Plum (light) / Soft Lilac (dark)
val PrimaryLight = Color(0xFF3A1444)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF522B5B)
val OnPrimaryContainerLight = Color(0xFFFCD6FF)

val PrimaryDark = Color(0xFFCDA0DE)
val OnPrimaryDark = Color(0xFF25112D)
val PrimaryContainerDark = Color(0xFF522B5B)
val OnPrimaryContainerDark = Color(0xFFFCD6FF)

// Secondary — Ethereal Purple
val SecondaryLight = Color(0xFF735A84)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFDFC0FD)
val OnSecondaryContainerLight = Color(0xFF2C1638)

val SecondaryDark = Color(0xFFB894C9)
val OnSecondaryDark = Color(0xFF2A1833)
val SecondaryContainerDark = Color(0xFF4B3658)
val OnSecondaryContainerDark = Color(0xFFEADAF3)

// Tertiary — Rich editorial plum
val TertiaryLight = Color(0xFF2D2035)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFE5D9ED)
val OnTertiaryContainerLight = Color(0xFF1C1123)

val TertiaryDark = Color(0xFFD8C4E0)
val OnTertiaryDark = Color(0xFF23172A)
val TertiaryContainerDark = Color(0xFF45324F)
val OnTertiaryContainerDark = Color(0xFFF1E6F5)

// Error
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFE57373)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF4A2525)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Background & Surface — Light Theme (cozy editorial)
val BackgroundLight = Color(0xFFFCF9F4)
val SurfaceLight = Color(0xFFFCF9F4)
val SurfaceVariantLight = Color(0xFFE8E0EA)
val OnBackgroundLight = Color(0xFF261D27)
val OnSurfaceLight = Color(0xFF261D27)
val OnSurfaceVariantLight = Color(0xFF4D444D)

// Surface container scale — Light Theme
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF6F2ED)
val SurfaceContainerLight = Color(0xFFF0EDE9)
val SurfaceContainerHighLight = Color(0xFFECE7E2)
val SurfaceContainerHighestLight = Color(0xFFE7E1DB)

// Background & Surface — Dark Theme
val BackgroundDark = Color(0xFF14161D)
val SurfaceDark = Color(0xFF1A1B1C)
val SurfaceVariantDark = Color(0xFF343038)
val OnBackgroundDark = Color(0xFFEDE8EE)
val OnSurfaceDark = Color(0xFFEDE8EE)
val OnSurfaceVariantDark = Color(0xFFB6ADB8)

// Surface container scale — Dark Theme
val SurfaceContainerLowestDark = Color(0xFF141516)
val SurfaceContainerLowDark = Color(0xFF1C1D1E)
val SurfaceContainerDark = Color(0xFF202123)
val SurfaceContainerHighDark = Color(0xFF25262A)
val SurfaceContainerHighestDark = Color(0xFF2B2D31)

// Outline
val OutlineLight = Color(0xFF897C88)
val OutlineVariantLight = Color(0xFFD0C3CD)
val OutlineDark = Color(0xFFA79FAF)
val OutlineVariantDark = Color(0xFF3E3945)

// Inverse
val InverseSurfaceLight = Color(0xFF2F2335)
val InverseOnSurfaceLight = Color(0xFFF7EFF7)
val InversePrimaryLight = Color(0xFFD6B2DF)

val InverseSurfaceDark = Color(0xFFEDE8EE)
val InverseOnSurfaceDark = Color(0xFF1E1A22)
val InversePrimaryDark = Color(0xFF3A1444)

// Section icon colors — Unified purple palette (all domains use lavender/purple)
val SectionIconBg = SecondaryContainerLight
val SectionIconBgDark = SecondaryContainerDark
val SectionIconTint = PrimaryLight
val SectionIconTintDark = PrimaryDark

// Legacy aliases — point to unified purple for backward compatibility
val WeightSectionBg = SectionIconBg
val WeightSectionBgDark = SectionIconBgDark
val WeightSectionTint = SectionIconTint
val WeightSectionTintDark = SectionIconTintDark

val VaccinationSectionBg = SectionIconBg
val VaccinationSectionBgDark = SectionIconBgDark
val VaccinationSectionTint = SectionIconTint
val VaccinationSectionTintDark = SectionIconTintDark

val DewormingSectionBg = SectionIconBg
val DewormingSectionBgDark = SectionIconBgDark
val DewormingSectionTint = SectionIconTint
val DewormingSectionTintDark = SectionIconTintDark

// Timeline event backgrounds — soft tints
val TimelineWeightBg = Color(0xFFF3E8F8)
val TimelineWeightBgDark = Color(0xFF31243A)
val TimelineVaccineBg = Color(0xFFF1E5F7)
val TimelineVaccineBgDark = Color(0xFF302339)
val TimelineDewormingBg = Color(0xFFF0E3F5)
val TimelineDewormingBgDark = Color(0xFF2E2236)
val TimelineReminderBg = Color(0xFFF6ECFA)
val TimelineReminderBgDark = Color(0xFF34263E)

// Weight change indicators
val WeightGain = Color(0xFF728625) // Olive green (tertiary)
val WeightGainDark = Color(0xFFB5C96A)
val WeightLoss = Color(0xFFC62828)
val WeightLossDark = Color(0xFFEF5350)

// Vaccine type indicator colors (harmonized with purple palette)
val VaccineV3Color = Color(0xFF522B5B) // Primary purple
val VaccineV4Color = Color(0xFF3B665A) // Teal
val VaccineV5Color = Color(0xFF9D81BA) // Secondary lavender
val VaccineRabiesColor = Color(0xFFC62828) // Red
val VaccineFelvColor = Color(0xFFCD8832) // Amber
val VaccineFivColor = Color(0xFF728625) // Olive green (tertiary)
val VaccineOtherColor = Color(0xFF7A757E) // Neutral gray

package com.wasimaster.wmkeyboard.core.theme

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reading a Kyng theme bundle (`.kyngtheme` or Kyng JSON/ZIP format).
 *
 * A Kyng theme definition combines:
 * - `theme`: metadata (`id`, `name`, `styleId`, `paletteId`, `soundId`, `effectId`, `lightingId`)
 * - `palette`: color roles (`board`, `key`, `keyPressed`, `keySpecial`, `label`, `accent`, `border`, `shadow`)
 * - `style`: tokens (`cornerRadiusDp`, `borderWidthDp`, `shape`, etc.)
 * - `skin`: multi-layer vector SVG keycap art matching [KeySkinSpec]
 */
object KyngTheme {

    const val FILE_EXTENSION = "kyngtheme"
    val IMPORT_MIME_TYPES = arrayOf("application/json", "application/zip", "application/octet-stream", "*/*")

    private val json = Json { ignoreUnknownKeys = true }

    fun looksLikeKyngTheme(text: String): Boolean {
        val trimmed = text.trimStart()
        if (!trimmed.startsWith("{")) return false
        return (trimmed.contains("\"catalog.v1\"") && (trimmed.contains("\"kind\": \"theme\"") || trimmed.contains("\"kind\":\"theme\""))) ||
            (trimmed.contains("\"styleId\"") && trimmed.contains("\"paletteId\"")) ||
            trimmed.contains("\"kyng\"")
    }

    /**
     * Parses a hex color string like `#FF120A24` or `#120A24` into an ARGB [Long].
     */
    fun parseColor(hex: String?): Long? {
        if (hex == null) return null
        val clean = hex.trim().removePrefix("#")
        return when (clean.length) {
            6 -> ("FF$clean".toLongOrNull(16) ?: return null)
            8 -> (clean.toLongOrNull(16) ?: return null)
            else -> null
        }
    }

    /**
     * Converts a Kyng bundle JSON into a [ThemeSpec].
     */
    fun read(text: String): KyngThemeResult {
        if (!looksLikeKyngTheme(text)) return KyngThemeResult.NotATheme
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: return KyngThemeResult.NotATheme

        val themeObj = root["theme"]?.jsonObject ?: root
        val paletteObj = root["palette"]?.jsonObject?.get("roles")?.jsonObject
            ?: root["roles"]?.jsonObject

        val styleObj = root["style"]?.jsonObject
        val tokens = styleObj?.get("tokens")?.jsonObject

        val skinElement = root["skin"] ?: root["keySkin"]
        val keySkinSpec: KeySkinSpec? = skinElement?.let {
            runCatching { json.decodeFromJsonElement(KeySkinSpec.serializer(), it) }.getOrNull()
        }

        val id = themeObj["id"]?.jsonPrimitive?.contentOrNull ?: "kyng_theme"
        val name = themeObj["name"]?.jsonPrimitive?.contentOrNull?.ifBlank { "Kyng Theme" } ?: "Kyng Theme"

        val boardColor = parseColor(paletteObj?.get("board")?.jsonPrimitive?.contentOrNull) ?: 0xFF17181CL
        val keyColor = parseColor(paletteObj?.get("key")?.jsonPrimitive?.contentOrNull) ?: 0xFF303338L
        val keyPressedColor = parseColor(paletteObj?.get("keyPressed")?.jsonPrimitive?.contentOrNull)
        val specialColor = parseColor(paletteObj?.get("keySpecial")?.jsonPrimitive?.contentOrNull)
        val specialPressedColor = parseColor(paletteObj?.get("keySpecialPressed")?.jsonPrimitive?.contentOrNull)
        val labelColor = parseColor(paletteObj?.get("label")?.jsonPrimitive?.contentOrNull) ?: 0xFFFFFFFFL
        val labelSecondaryColor = parseColor(paletteObj?.get("labelSecondary")?.jsonPrimitive?.contentOrNull)
        val accentColor = parseColor(paletteObj?.get("accent")?.jsonPrimitive?.contentOrNull) ?: 0xFF8AB4F8L
        val borderColor = parseColor(paletteObj?.get("border")?.jsonPrimitive?.contentOrNull)

        val cornerRadiusDp = tokens?.get("cornerRadiusDp")?.jsonPrimitive?.floatOrNull ?: 10f
        val borderWidthDp = tokens?.get("borderWidthDp")?.jsonPrimitive?.floatOrNull ?: 0f

        val shapeStr = tokens?.get("shape")?.jsonPrimitive?.contentOrNull?.uppercase()
        val keyShapeKind = when (shapeStr) {
            "PILL", "CAPSULE" -> KeyShapeKind.PILL
            "RECT", "SQUARE" -> if (cornerRadiusDp > 0f) KeyShapeKind.ROUNDED else KeyShapeKind.SHARP
            "CUT" -> KeyShapeKind.CUT
            "CIRCLE" -> KeyShapeKind.CIRCLE
            else -> KeyShapeKind.ROUNDED
        }

        val spec = ThemeSpec(
            id = "kyng_$id",
            name = name,
            dark = true,
            boardBackground = boardColor,
            keyBackground = keyColor,
            keyText = labelColor,
            modifierKeyBackground = specialColor ?: keyColor,
            modifierKeyText = labelColor,
            enterKeyBackground = specialColor ?: accentColor,
            enterKeyText = labelColor,
            pressedKeyBackground = keyPressedColor ?: keyColor,
            accent = accentColor,
            hintText = labelSecondaryColor ?: labelColor,
            secondaryText = labelSecondaryColor ?: labelColor,
            keyBorderColor = borderColor ?: 0x00000000L,
            keyBorderWidthDp = borderWidthDp,
            keyCornerRadiusDp = cornerRadiusDp.toInt(),
            keyShape = keyShapeKind,
            keySkin = keySkinSpec,
        )

        return KyngThemeResult.Converted(spec)
    }
}

sealed interface KyngThemeResult {
    data class Converted(val theme: ThemeSpec) : KyngThemeResult
    data object NotATheme : KyngThemeResult
}

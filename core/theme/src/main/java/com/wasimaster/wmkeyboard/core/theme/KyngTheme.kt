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

        val effectId = (themeObj["effectId"] ?: root["effectId"])?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
        val styleId = (themeObj["styleId"] ?: root["styleId"])?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()

        val keyLighting = when {
            effectId in listOf("ripple", "sonar") -> KeyLightingSpec(
                pattern = "RIPPLE",
                target = "GLOW",
                intensity = 0.85f,
                sizeDp = 14f,
                speed = 1.2f,
                decayMs = 650
            )
            effectId in listOf("bubbles", "bubble-pop") -> KeyLightingSpec(
                pattern = "RIPPLE",
                target = "GLOW",
                intensity = 0.75f,
                sizeDp = 10f,
                speed = 1f,
                decayMs = 600
            )
            effectId in listOf("confetti", "sparks", "starburst", "shockwave", "seal") -> KeyLightingSpec(
                pattern = "SPLASH",
                target = "EDGE",
                intensity = 0.9f,
                sizeDp = 10f,
                speed = 1.3f,
                decayMs = 500
            )
            effectId == "glitch" -> KeyLightingSpec(
                pattern = "TWINKLE",
                target = "FACE",
                intensity = 0.85f,
                speed = 2f,
                decayMs = 400
            )
            effectId in listOf("slash", "claw", "strand") -> KeyLightingSpec(
                pattern = "CROSS",
                target = "FACE",
                intensity = 0.8f,
                decayMs = 500
            )
            effectId in listOf("stormcall", "zap", "flame", "wyrmfire") -> KeyLightingSpec(
                pattern = "SPLASH",
                target = "GLOW",
                intensity = 0.9f,
                sizeDp = 12f,
                decayMs = 600
            )
            effectId in listOf("retro", "pixels") -> KeyLightingSpec(
                pattern = "KEY",
                target = "FACE",
                intensity = 0.85f,
                decayMs = 500
            )
            "cyberpunk" in id || "rgb" in id -> KeyLightingSpec(
                pattern = "WAVE",
                target = "GLOW",
                intensity = 0.85f,
                hueSpanDeg = 360f,
                speed = 1.2f
            )
            "rain" in id || effectId == "rain" -> KeyLightingSpec(
                pattern = "RAIN",
                target = "GLOW",
                intensity = 0.7f,
                speed = 1.2f
            )
            else -> KeyLightingSpec(
                pattern = "KEY",
                target = "GLOW",
                intensity = 0.75f,
                sizeDp = 10f,
                decayMs = 600
            )
        }

        val boardEffect = when {
            effectId in listOf("bubbles", "bubble-pop") || "bubble" in id || "water" in id -> BoardEffectSpec(
                kind = "BUBBLES",
                density = 0.6f,
                speed = 1f,
                intensity = 0.65f,
                scale = 1f,
                reactive = 1f,
                halfLifeMs = 1200
            )
            effectId in listOf("ripple", "sonar") || "frost" in id || "jelly" in id -> BoardEffectSpec(
                kind = "RIPPLES",
                density = 0.5f,
                speed = 1f,
                intensity = 0.6f,
                scale = 1f,
                reactive = 1f,
                halfLifeMs = 1000
            )
            effectId == "confetti" || "spraycan" in id -> BoardEffectSpec(
                kind = "CONFETTI",
                density = 0.65f,
                speed = 1.1f,
                intensity = 0.7f,
                reactive = 1f,
                halfLifeMs = 1500
            )
            effectId in listOf("stormcall", "zap", "flame", "wyrmfire") || "ember" in id || "obsidian" in id -> BoardEffectSpec(
                kind = "EMBERS",
                density = 0.6f,
                speed = 1f,
                intensity = 0.7f,
                reactive = 1f,
                halfLifeMs = 1200
            )
            "rain" in id || "searchlight" in id -> BoardEffectSpec(
                kind = "RAIN",
                density = 0.7f,
                speed = 1.3f,
                intensity = 0.65f,
                reactive = 1f
            )
            "terminal" in id || "matrix" in id -> BoardEffectSpec(
                kind = "CODE_RAIN",
                density = 0.65f,
                speed = 1f,
                intensity = 0.6f,
                reactive = 1f
            )
            "aurora" in id || "holo" in id || "veil" in id -> BoardEffectSpec(
                kind = "AURORA",
                density = 0.5f,
                speed = 0.8f,
                intensity = 0.55f,
                reactive = 0.8f
            )
            effectId == "starburst" || "star" in id || "crown" in id || "gilded" in id || "x07" in id -> BoardEffectSpec(
                kind = "STARFIELD",
                density = 0.5f,
                speed = 0.7f,
                intensity = 0.5f,
                reactive = 0.9f
            )
            effectId in listOf("pixels", "retro") -> BoardEffectSpec(
                kind = "DOT_GRID",
                density = 0.6f,
                intensity = 0.45f,
                reactive = 0.8f
            )
            else -> null
        }

        val keyMaterial = when {
            listOf("alloy", "armor", "tactical", "stormforge", "vanguard", "steel", "machined").any { it in styleId } -> KeyMaterialSpec(kind = "METAL")
            listOf("cyberpunk", "hazard", "saber", "neon").any { it in styleId } -> KeyMaterialSpec(kind = "NEON")
            listOf("frost", "clear", "glass").any { it in styleId } -> KeyMaterialSpec(kind = "GLASS")
            "typewriter" in styleId -> KeyMaterialSpec(kind = "TYPEWRITER")
            listOf("timber", "wood", "oak").any { it in styleId } -> KeyMaterialSpec(kind = "WOOD")
            listOf("ceramic", "glaze", "pearl", "porcelain").any { it in styleId } -> KeyMaterialSpec(kind = "CERAMIC")
            listOf("leather", "wisecrack").any { it in styleId } -> KeyMaterialSpec(kind = "LEATHER")
            listOf("terminal", "dot-matrix").any { it in styleId } -> KeyMaterialSpec(kind = "TERMINAL")
            listOf("bubble", "charm", "marshmallow", "crayon").any { it in styleId } -> KeyMaterialSpec(kind = "CARTOON")
            listOf("voxel", "arcade").any { it in styleId } -> KeyMaterialSpec(kind = "ARCADE")
            listOf("basalt", "rune", "stone", "brutalist").any { it in styleId } -> KeyMaterialSpec(kind = "STONE")
            listOf("felt", "origami", "paper").any { it in styleId } -> KeyMaterialSpec(kind = "PAPER")
            listOf("jelly", "cushion", "candy").any { it in styleId } -> KeyMaterialSpec(kind = "CANDY")
            else -> null
        }

        val keyEffect = when {
            effectId == "confetti" || "spraycan" in id -> "CONFETTI"
            effectId in listOf("charm", "paws") || "star" in id -> "STARS"
            effectId in listOf("k-pop") || "heart" in id -> "HEARTS"
            effectId in listOf("bubbles", "bubble-pop") -> "EMOJI"
            else -> "SPARKLE"
        }
        val keyEffectParam = if (effectId in listOf("bubbles", "bubble-pop")) "🫧" else null

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
            keyLighting = keyLighting,
            boardEffect = boardEffect,
            keyMaterial = keyMaterial,
            keyEffect = keyEffect,
            keyEffectParam = keyEffectParam,
            keyEffectIntensity = 1f,
            keyEffectSize = 1.2f,
            keyEffectSpeed = 1f,
            keyEffectSpread = 0.8f,
        )

        return KyngThemeResult.Converted(spec)
    }
}

sealed interface KyngThemeResult {
    data class Converted(val theme: ThemeSpec) : KyngThemeResult
    data object NotATheme : KyngThemeResult
}

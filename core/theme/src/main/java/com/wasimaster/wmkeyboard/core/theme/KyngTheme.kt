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

        val keyLighting = when (effectId) {
            "block-break" -> KeyLightingSpec(pattern = "CROSS", target = "FACE", intensity = 0.7f, decayMs = 400)
            "bubble-pop", "bubbles" -> KeyLightingSpec(pattern = "RIPPLE", target = "GLOW", intensity = 0.65f, sizeDp = 8f, speed = 1f, decayMs = 500)
            "claw", "slash" -> KeyLightingSpec(pattern = "CROSS", target = "FACE", intensity = 0.8f, decayMs = 400)
            "confetti" -> KeyLightingSpec(pattern = "SPLASH", target = "EDGE", intensity = 0.75f, sizeDp = 6f, speed = 1.2f, decayMs = 450)
            "echo", "sonar" -> KeyLightingSpec(pattern = "ECHO", target = "GLOW", intensity = 0.7f, sizeDp = 8f, speed = 1.1f, decayMs = 550)
            "flame", "wyrmfire" -> KeyLightingSpec(pattern = "SPLASH", target = "GLOW", intensity = 0.75f, sizeDp = 8f, speed = 1.2f, decayMs = 500)
            "focus" -> KeyLightingSpec(pattern = "KEY", target = "EDGE", intensity = 0.75f, decayMs = 400)
            "fog" -> KeyLightingSpec(pattern = "BREATHE", target = "GLOW", intensity = 0.55f, sizeDp = 6f, speed = 1f, decayMs = 500)
            "glint", "shine" -> KeyLightingSpec(pattern = "TWINKLE", target = "FACE", intensity = 0.75f, speed = 1.6f, decayMs = 400)
            "glitch" -> KeyLightingSpec(pattern = "TWINKLE", target = "FACE", intensity = 0.8f, speed = 2f, decayMs = 300)
            "glow", "press-glow" -> KeyLightingSpec(pattern = "KEY", target = "GLOW", intensity = 0.7f, sizeDp = 6f, decayMs = 500)
            "gold-dust" -> KeyLightingSpec(pattern = "SPLASH", target = "GLOW", intensity = 0.7f, sizeDp = 6f, speed = 1.1f, decayMs = 500)
            "lightspeed" -> KeyLightingSpec(pattern = "SWEEP", target = "FACE", intensity = 0.75f, speed = 1.4f, decayMs = 400)
            "none" -> null
            "notes" -> KeyLightingSpec(pattern = "SPLASH", target = "GLOW", intensity = 0.7f, sizeDp = 6f, speed = 1.1f, decayMs = 500)
            "paw-print" -> KeyLightingSpec(pattern = "KEY", target = "FACE", intensity = 0.75f, decayMs = 450)
            "pixels", "retro" -> KeyLightingSpec(pattern = "KEY", target = "FACE", intensity = 0.75f, decayMs = 400)
            "recoil" -> KeyLightingSpec(pattern = "RIPPLE", target = "EDGE", intensity = 0.75f, sizeDp = 6f, speed = 1.2f, decayMs = 450)
            "ripple" -> KeyLightingSpec(pattern = "RIPPLE", target = "GLOW", intensity = 0.7f, sizeDp = 8f, speed = 1.1f, decayMs = 550)
            "runes" -> KeyLightingSpec(pattern = "CROSS", target = "FACE", intensity = 0.75f, decayMs = 450)
            "seal" -> KeyLightingSpec(pattern = "KEY", target = "EDGE", intensity = 0.75f, decayMs = 450)
            "shockwave", "sparks" -> KeyLightingSpec(pattern = "SPLASH", target = "EDGE", intensity = 0.8f, sizeDp = 6f, speed = 1.2f, decayMs = 450)
            "starburst" -> KeyLightingSpec(pattern = "SPLASH", target = "GLOW", intensity = 0.75f, sizeDp = 8f, speed = 1.2f, decayMs = 500)
            "stormcall", "zap" -> KeyLightingSpec(pattern = "SPLASH", target = "GLOW", intensity = 0.8f, sizeDp = 8f, speed = 1.2f, decayMs = 500)
            "strand" -> KeyLightingSpec(pattern = "CROSS", target = "EDGE", intensity = 0.75f, decayMs = 450)
            "typewriter" -> KeyLightingSpec(pattern = "KEY", target = "FACE", intensity = 0.75f, decayMs = 350)
            else -> when {
                "cyberpunk" in id || "rgb" in id -> KeyLightingSpec(pattern = "WAVE", target = "GLOW", intensity = 0.75f, hueSpanDeg = 360f, speed = 1.1f)
                "rain" in id -> KeyLightingSpec(pattern = "RAIN", target = "GLOW", intensity = 0.65f, speed = 1.1f)
                else -> null
            }
        }

        val boardEffect = when (effectId) {
            "block-break", "runes" -> BoardEffectSpec(kind = "CONTOUR", density = 0.6f, speed = 1f, intensity = 0.6f, reactive = 1f)
            "bubble-pop", "bubbles" -> BoardEffectSpec(kind = "BUBBLES", density = 0.65f, speed = 1f, intensity = 0.65f, scale = 1f, reactive = 1f, halfLifeMs = 1200)
            "claw", "slash", "glint", "paw-print", "shine" -> BoardEffectSpec(kind = "SHEEN", density = 0.6f, speed = 1.1f, intensity = 0.65f, reactive = 1f)
            "confetti" -> BoardEffectSpec(kind = "CONFETTI", density = 0.65f, speed = 1.1f, intensity = 0.7f, reactive = 1f, halfLifeMs = 1500)
            "echo", "recoil", "ripple" -> BoardEffectSpec(kind = "RIPPLES", density = 0.5f, speed = 1f, intensity = 0.6f, scale = 1f, reactive = 1f, halfLifeMs = 1000)
            "flame" -> BoardEffectSpec(kind = "LAVA", density = 0.6f, speed = 1f, intensity = 0.7f, reactive = 1f, halfLifeMs = 1200)
            "focus" -> BoardEffectSpec(kind = "SCAN", density = 0.6f, speed = 1.2f, intensity = 0.65f, reactive = 1f)
            "fog" -> BoardEffectSpec(kind = "SMOKE", density = 0.6f, speed = 0.8f, intensity = 0.6f, reactive = 0.8f)
            "glitch" -> BoardEffectSpec(kind = "CODE_RAIN", density = 0.65f, speed = 1.1f, intensity = 0.65f, reactive = 1f)
            "glow" -> BoardEffectSpec(kind = "AURORA", density = 0.5f, speed = 0.8f, intensity = 0.55f, reactive = 0.8f)
            "gold-dust" -> BoardEffectSpec(kind = "MOTES", density = 0.6f, speed = 0.9f, intensity = 0.65f, reactive = 1f)
            "lightspeed" -> BoardEffectSpec(kind = "WARP", density = 0.6f, speed = 1.2f, intensity = 0.7f, reactive = 1f)
            "notes" -> BoardEffectSpec(kind = "EQUALIZER", density = 0.65f, speed = 1f, intensity = 0.6f, reactive = 1f)
            "pixels", "retro" -> BoardEffectSpec(kind = "DOT_GRID", density = 0.6f, intensity = 0.5f, reactive = 0.9f)
            "press-glow" -> BoardEffectSpec(kind = "BOKEH", density = 0.5f, speed = 0.8f, intensity = 0.5f, reactive = 0.8f)
            "shockwave", "sonar" -> BoardEffectSpec(kind = "RIPPLES", density = 0.6f, speed = 1.3f, intensity = 0.7f, reactive = 1.2f)
            "sparks" -> BoardEffectSpec(kind = "FIREWORKS", density = 0.6f, speed = 1.1f, intensity = 0.7f, reactive = 1f)
            "starburst" -> BoardEffectSpec(kind = "STARFIELD", density = 0.5f, speed = 0.7f, intensity = 0.5f, reactive = 1f)
            "stormcall" -> BoardEffectSpec(kind = "NEBULA", density = 0.55f, speed = 1f, intensity = 0.65f, reactive = 1f)
            "strand" -> BoardEffectSpec(kind = "CONTOUR", density = 0.5f, speed = 0.8f, intensity = 0.55f, reactive = 0.8f)
            "wyrmfire" -> BoardEffectSpec(kind = "EMBERS", density = 0.65f, speed = 1.1f, intensity = 0.75f, reactive = 1f, halfLifeMs = 1200)
            "zap" -> BoardEffectSpec(kind = "AURORA", density = 0.6f, speed = 1f, intensity = 0.65f, reactive = 1f)
            "none", "typewriter", "seal" -> null
            else -> when {
                "rain" in id || "searchlight" in id -> BoardEffectSpec(kind = "RAIN", density = 0.7f, speed = 1.3f, intensity = 0.65f, reactive = 1f)
                "terminal" in id || "matrix" in id -> BoardEffectSpec(kind = "CODE_RAIN", density = 0.65f, speed = 1f, intensity = 0.6f, reactive = 1f)
                "aurora" in id || "holo" in id || "veil" in id -> BoardEffectSpec(kind = "AURORA", density = 0.5f, speed = 0.8f, intensity = 0.55f, reactive = 0.8f)
                "star" in id || "crown" in id || "gilded" in id || "x07" in id -> BoardEffectSpec(kind = "STARFIELD", density = 0.5f, speed = 0.7f, intensity = 0.5f, reactive = 0.9f)
                else -> null
            }
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

        val (keyEffect, keyEffectParam) = when (effectId) {
            "confetti" -> "CONFETTI" to null
            "glint", "shine", "starburst", "charm" -> "STARS" to null
            "k-pop" -> "HEARTS" to null
            "glow", "press-glow", "sparks", "zap", "flame", "wyrmfire", "stormcall", "echo", "recoil", "ripple", "focus" -> "SPARKLE" to null
            "none" -> null to null
            else -> when {
                "spraycan" in id -> "CONFETTI" to null
                "charm" in id || "star" in id -> "STARS" to null
                "k-pop" in id || "heart" in id -> "HEARTS" to null
                else -> null
            }
        }

                val adjustedSkinSpec = keySkinSpec?.let { spec ->
            val popupSlot = spec.slots["popup"]
            if (popupSlot != null && popupSlot.stretchX == null && popupSlot.stretchY == null) {
                val w = popupSlot.size.getOrNull(0) ?: 150f
                val h = popupSlot.size.getOrNull(1) ?: 160f
                val updatedSlots = spec.slots.toMutableMap()
                updatedSlots["popup"] = popupSlot.copy(
                    stretchX = listOf(w * 0.2f, w * 0.8f),
                    stretchY = listOf(h * 0.2f, h * 0.8f),
                )
                spec.copy(slots = updatedSlots)
            } else {
                spec
            }
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
            keySkin = adjustedSkinSpec,
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

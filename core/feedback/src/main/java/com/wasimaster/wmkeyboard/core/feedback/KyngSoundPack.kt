package com.wasimaster.wmkeyboard.core.feedback

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.pow

/**
 * Reads the sound pack `pack.json` published by Kyng Keyboard (`com.aionyxe.kyng`),
 * so sound packs from that keyboard can be imported as they are into wmkeyboard.
 *
 * Kyng uses a `schema: "pack.v2"` format with event lists under `press`:
 * - `letter` -> top-level `press`
 * - `space`  -> role `space`
 * - `enter`  -> role `enter`
 * - `delete` -> role `delete`
 * - `modifier` -> role `modifier`
 *
 * It specifies gain in dB (`gainDb`), which this converts to a linear 0.0..1.0 gain factor.
 */
object KyngSoundPack {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class KyngPackEvents(
        val letter: List<String> = emptyList(),
        val space: List<String> = emptyList(),
        val enter: List<String> = emptyList(),
        val delete: List<String> = emptyList(),
        val modifier: List<String> = emptyList(),
    )

    @Serializable
    private data class KyngManifest(
        val schema: String = "",
        val id: String = "",
        val rateMin: Float? = null,
        val rateMax: Float? = null,
        val gainDb: Float = 0f,
        val press: KyngPackEvents = KyngPackEvents(),
        val release: KyngPackEvents = KyngPackEvents(),
    )

    /**
     * Whether [manifest] claims Kyng's sound pack format.
     */
    fun looksLike(manifest: String): Boolean {
        val trimmed = manifest.trimStart()
        if (!trimmed.startsWith("{")) return false
        return (manifest.contains("\"pack.v2\"") || manifest.contains("\"gainDb\"")) &&
            manifest.contains("\"letter\"")
    }

    /**
     * Converts a Kyng sound pack manifest string into wmkeyboard's [SoundPackManifest].
     */
    fun toManifest(manifest: String): SoundPackManifest? {
        if (!looksLike(manifest)) return null
        val source = runCatching { json.decodeFromString<KyngManifest>(manifest) }.getOrNull()
            ?: return null

        val defaultPress = source.press.letter
        val defaultRelease = source.release.letter

        // If there are no letter sounds and no space/enter/delete sounds at all, refuse
        if (defaultPress.isEmpty() && source.press.space.isEmpty() && source.press.enter.isEmpty()) {
            return null
        }

        // Convert gainDb to linear gain: linear = 10^(gainDb / 20), clamped to 0.0..1.0
        val linearGain = if (source.gainDb <= 0f) {
            10.0.pow(source.gainDb.toDouble() / 20.0).toFloat().coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val roles = mutableMapOf<String, SoundPackRoleSpec>()

        if (source.press.space.isNotEmpty() || source.release.space.isNotEmpty()) {
            roles[KeySoundRole.SPACE.serialName] = SoundPackRoleSpec(
                press = source.press.space,
                release = source.release.space,
            )
        }
        if (source.press.enter.isNotEmpty() || source.release.enter.isNotEmpty()) {
            roles[KeySoundRole.ENTER.serialName] = SoundPackRoleSpec(
                press = source.press.enter,
                release = source.release.enter,
            )
        }
        if (source.press.delete.isNotEmpty() || source.release.delete.isNotEmpty()) {
            roles[KeySoundRole.DELETE.serialName] = SoundPackRoleSpec(
                press = source.press.delete,
                release = source.release.delete,
            )
        }
        if (source.press.modifier.isNotEmpty() || source.release.modifier.isNotEmpty()) {
            roles[KeySoundRole.MODIFIER.serialName] = SoundPackRoleSpec(
                press = source.press.modifier,
                release = source.release.modifier,
            )
        }

        val name = source.id.replace('-', ' ')
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Kyng Sound Pack" }

        return SoundPackManifest(
            format = SoundPackFile.FORMAT,
            version = SoundPackFile.VERSION,
            id = source.id.ifBlank { "kyng-pack" },
            name = name,
            author = "Kyng contributors",
            packVersion = "1.0.0",
            description = "Imported from Kyng Keyboard sound pack ($name).",
            gain = linearGain,
            press = defaultPress,
            release = defaultRelease,
            roles = roles,
        )
    }
}

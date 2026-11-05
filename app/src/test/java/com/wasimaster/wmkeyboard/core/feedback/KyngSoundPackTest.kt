package com.wasimaster.wmkeyboard.core.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KyngSoundPackTest {

    private val kyngBrassMechJson = """
    {
      "schema": "pack.v2",
      "id": "brass-mech",
      "rateMin": 0.94,
      "rateMax": 1.06,
      "gainDb": 1.5,
      "placeholder": false,
      "press": {
        "letter": [
          "letter-1.ogg",
          "letter-2.ogg"
        ],
        "space": [
          "space-1.ogg"
        ],
        "enter": [
          "enter-1.ogg"
        ],
        "delete": [
          "delete-1.ogg"
        ],
        "modifier": [
          "modifier-1.ogg"
        ]
      },
      "release": {}
    }
    """.trimIndent()

    @Test
    fun `looksLike recognises Kyng sound packs`() {
        assertTrue(KyngSoundPack.looksLike(kyngBrassMechJson))
        assertFalse(KyngSoundPack.looksLike("""{"format": "wmkeyboard-sound-pack"}"""))
        assertFalse(KyngSoundPack.looksLike("""{"schemaVersion": 1, "keypress.default": {}}"""))
        assertFalse(KyngSoundPack.looksLike("plain text"))
    }

    @Test
    fun `toManifest translates Kyng pack to SoundPackManifest`() {
        val manifest = KyngSoundPack.toManifest(kyngBrassMechJson)
        assertNotNull(manifest)
        assertEquals(SoundPackFile.FORMAT, manifest!!.format)
        assertEquals("brass-mech", manifest.id)
        assertEquals("Brass Mech", manifest.name)
        assertEquals(listOf("letter-1.ogg", "letter-2.ogg"), manifest.press)
        assertEquals(1.0f, manifest.gain) // 1.5 dB is clamped to 1.0f

        val roles = manifest.roles
        assertEquals(listOf("space-1.ogg"), roles[KeySoundRole.SPACE.serialName]?.press)
        assertEquals(listOf("enter-1.ogg"), roles[KeySoundRole.ENTER.serialName]?.press)
        assertEquals(listOf("delete-1.ogg"), roles[KeySoundRole.DELETE.serialName]?.press)
        assertEquals(listOf("modifier-1.ogg"), roles[KeySoundRole.MODIFIER.serialName]?.press)
    }

    @Test
    fun `SoundPackFile recognises and parses Kyng manifests`() {
        assertTrue(SoundPackFile.recognises(kyngBrassMechJson))
    }
}

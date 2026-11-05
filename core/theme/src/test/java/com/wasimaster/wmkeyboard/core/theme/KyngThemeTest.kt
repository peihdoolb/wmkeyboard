package com.wasimaster.wmkeyboard.core.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KyngThemeTest {

    private val sampleKyngBundle = """
    {
      "schema": "catalog.v1",
      "kind": "theme",
      "id": "bubble-crayon",
      "name": "Bubble Crayon",
      "styleId": "bubble",
      "paletteId": "crayon",
      "roles": {
        "board": "#FFFCEFD6",
        "key": "#FFFFF7E8",
        "keyPressed": "#FFF6E2C4",
        "keySpecial": "#FFFCE6CB",
        "label": "#FF8C4614",
        "accent": "#FFF6A548",
        "border": "#FFE8C59A"
      },
      "style": {
        "tokens": {
          "cornerRadiusDp": 12.0,
          "borderWidthDp": 1.5,
          "shape": "rect"
        }
      },
      "skin": {
        "slots": {
          "letter": {
            "size": [100.0, 100.0],
            "layers": [
              {
                "d": "M0 0L100 100Z"
              }
            ]
          }
        }
      }
    }
    """.trimIndent()

    @Test
    fun `looksLikeKyngTheme identifies Kyng format`() {
        assertTrue(KyngTheme.looksLikeKyngTheme(sampleKyngBundle))
    }

    @Test
    fun `read converts Kyng theme bundle into ThemeSpec`() {
        val res = KyngTheme.read(sampleKyngBundle)
        assertTrue(res is KyngThemeResult.Converted)
        val theme = (res as KyngThemeResult.Converted).theme
        assertEquals("kyng_bubble-crayon", theme.id)
        assertEquals("Bubble Crayon", theme.name)
        assertEquals(0xFFFCEFD6L, theme.boardBackground)
        assertEquals(0xFFFFF7E8L, theme.keyBackground)
        assertEquals(0xFFF6A548L, theme.accent)
        assertEquals(12, theme.keyCornerRadiusDp)
        assertEquals(1.5f, theme.keyBorderWidthDp)
        assertEquals(KeyShapeKind.ROUNDED, theme.keyShape)
        assertNotNull(theme.keySkin)
        assertEquals(1, theme.keySkin?.slots?.size)
    }
}

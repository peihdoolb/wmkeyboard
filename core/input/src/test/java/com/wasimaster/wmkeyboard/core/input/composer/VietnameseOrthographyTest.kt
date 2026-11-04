package com.wasimaster.wmkeyboard.core.input.composer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VietnameseOrthographyTest {

    @Test
    fun `rule 1 - gh before i, e, ê is valid syllable`() {
        // Đi với âm i: cái ghim, ghìm xuống, ghi nhớ
        assertTrue(VietnameseOrthography.isSyllable("ghim"))
        assertTrue(VietnameseOrthography.isSyllable("ghìm"))
        assertTrue(VietnameseOrthography.isSyllable("ghi"))

        // Đi với âm e: cái ghe, con ghẹ, ghẻ lở, ghen ghét
        assertTrue(VietnameseOrthography.isSyllable("ghe"))
        assertTrue(VietnameseOrthography.isSyllable("ghẹ"))
        assertTrue(VietnameseOrthography.isSyllable("ghẻ"))
        assertTrue(VietnameseOrthography.isSyllable("ghen"))
        assertTrue(VietnameseOrthography.isSyllable("ghét"))

        // Đi với âm ê: cái ghế, ghê gớm, trêu ghẹo
        assertTrue(VietnameseOrthography.isSyllable("ghế"))
        assertTrue(VietnameseOrthography.isSyllable("ghê"))
        assertTrue(VietnameseOrthography.isSyllable("ghẹo"))
    }

    @Test
    fun `rule 1 - g before e, ê or invalid i is corrected to gh`() {
        // Đi với âm i: gim -> ghim, gìm -> ghìm
        assertEquals("ghim", VietnameseOrthography.correctSyllable("gim"))
        assertEquals("ghìm", VietnameseOrthography.correctSyllable("gìm"))

        // Đi với âm e: cái ghe (ge), con ghẹ (gẹ), ghẻ lở (gẻ), ghen ghét (gen, gét)
        assertEquals("ghe", VietnameseOrthography.correctSyllable("ge"))
        assertEquals("ghẹ", VietnameseOrthography.correctSyllable("gẹ"))
        assertEquals("ghẻ", VietnameseOrthography.correctSyllable("gẻ"))
        assertEquals("ghen", VietnameseOrthography.correctSyllable("gen"))
        assertEquals("ghét", VietnameseOrthography.correctSyllable("gét"))

        // Đi với âm ê: cái ghế (gế), ghê gớm (gê), trêu ghẹo (gẹo)
        assertEquals("ghế", VietnameseOrthography.correctSyllable("gế"))
        assertEquals("ghê", VietnameseOrthography.correctSyllable("gê"))
        assertEquals("ghẹo", VietnameseOrthography.correctSyllable("gẹo"))

        // Invalid syllables
        assertFalse(VietnameseOrthography.isSyllable("ge"))
        assertFalse(VietnameseOrthography.isSyllable("gẹ"))
        assertFalse(VietnameseOrthography.isSyllable("gê"))
        assertFalse(VietnameseOrthography.isSyllable("gế"))
        assertFalse(VietnameseOrthography.isSyllable("gim"))
    }

    @Test
    fun `rule 2 - g is used before all remaining vowels a, ă, â, o, ô, ơ, u, ư`() {
        // Đi với âm a: con gà, hạt gạo, gang thép
        assertTrue(VietnameseOrthography.isSyllable("gà"))
        assertTrue(VietnameseOrthography.isSyllable("gạo"))
        assertTrue(VietnameseOrthography.isSyllable("gang"))

        // Đi với âm o: gõ cửa, gom góp
        assertTrue(VietnameseOrthography.isSyllable("gõ"))
        assertTrue(VietnameseOrthography.isSyllable("gom"))
        assertTrue(VietnameseOrthography.isSyllable("góp"))

        // Đi với âm ô: cái gối, gỗ lụa, gốm sứ
        assertTrue(VietnameseOrthography.isSyllable("gối"))
        assertTrue(VietnameseOrthography.isSyllable("gỗ"))
        assertTrue(VietnameseOrthography.isSyllable("gốm"))

        // Đi với âm ơ: gỡ rối
        assertTrue(VietnameseOrthography.isSyllable("gỡ"))

        // Đi với âm u, ư: gù lưng, gửi, gữi, gừng
        assertTrue(VietnameseOrthography.isSyllable("gù"))
        assertTrue(VietnameseOrthography.isSyllable("gửi"))
        assertTrue(VietnameseOrthography.isSyllable("gữi"))
        assertTrue(VietnameseOrthography.isSyllable("gừng"))
    }

    @Test
    fun `rule 2 - gh before remaining vowels is corrected to g`() {
        // Đi với âm a: ghà -> gà, ghạo -> gạo, ghang -> gang
        assertEquals("gà", VietnameseOrthography.correctSyllable("ghà"))
        assertEquals("gạo", VietnameseOrthography.correctSyllable("ghạo"))
        assertEquals("gang", VietnameseOrthography.correctSyllable("ghang"))

        // Đi với âm o: ghõ -> gõ, ghom -> gom, ghóp -> góp
        assertEquals("gõ", VietnameseOrthography.correctSyllable("ghõ"))
        assertEquals("gom", VietnameseOrthography.correctSyllable("ghom"))
        assertEquals("góp", VietnameseOrthography.correctSyllable("ghóp"))

        // Đi với âm ô: ghối -> gối, ghỗ -> gỗ, ghốm -> gốm
        assertEquals("gối", VietnameseOrthography.correctSyllable("ghối"))
        assertEquals("gỗ", VietnameseOrthography.correctSyllable("ghỗ"))
        assertEquals("gốm", VietnameseOrthography.correctSyllable("ghốm"))

        // Đi với âm ơ: ghỡ -> gỡ
        assertEquals("gỡ", VietnameseOrthography.correctSyllable("ghỡ"))

        // Đi với âm u, ư: ghù -> gù, ghửi -> gửi, ghữi -> gữi, ghừng -> gừng
        assertEquals("gù", VietnameseOrthography.correctSyllable("ghù"))
        assertEquals("gửi", VietnameseOrthography.correctSyllable("ghửi"))
        assertEquals("gữi", VietnameseOrthography.correctSyllable("ghữi"))
        assertEquals("gừng", VietnameseOrthography.correctSyllable("ghừng"))

        // gh before remaining vowels are not valid syllables
        assertFalse(VietnameseOrthography.isSyllable("gha"))
        assertFalse(VietnameseOrthography.isSyllable("ghà"))
        assertFalse(VietnameseOrthography.isSyllable("gho"))
        assertFalse(VietnameseOrthography.isSyllable("ghõ"))
        assertFalse(VietnameseOrthography.isSyllable("ghu"))
        assertFalse(VietnameseOrthography.isSyllable("ghù"))
        assertFalse(VietnameseOrthography.isSyllable("ghừng"))
    }

    @Test
    fun `gi words like gì, gìn, gỉ are preserved and not changed to gh`() {
        assertNull(VietnameseOrthography.correctSyllable("gì"))
        assertNull(VietnameseOrthography.correctSyllable("gìn"))
        assertNull(VietnameseOrthography.correctSyllable("gỉ"))
        assertNull(VietnameseOrthography.correctSyllable("gi"))

        assertTrue(VietnameseOrthography.isSyllable("gì"))
        assertTrue(VietnameseOrthography.isSyllable("gìn"))
        assertTrue(VietnameseOrthography.isSyllable("gỉ"))
        assertTrue(VietnameseOrthography.isSyllable("gi"))
    }

    @Test
    fun `capitalization is preserved during correction`() {
        assertEquals("Gà", VietnameseOrthography.correctSyllable("Ghà"))
        assertEquals("GÀ", VietnameseOrthography.correctSyllable("GHÀ"))
        assertEquals("Ghe", VietnameseOrthography.correctSyllable("Ge"))
        assertEquals("GHE", VietnameseOrthography.correctSyllable("GE"))
        assertEquals("Ghim", VietnameseOrthography.correctSyllable("Gim"))
    }

    @Test
    fun `ng and ngh follow the same orthography rules`() {
        assertEquals("nghiêng", VietnameseOrthography.correctSyllable("ngiêng"))
        assertEquals("nghe", VietnameseOrthography.correctSyllable("nge"))
        assertEquals("ngà", VietnameseOrthography.correctSyllable("nghà"))
        assertEquals("ngủ", VietnameseOrthography.correctSyllable("nghủ"))
        assertEquals("người", VietnameseOrthography.correctSyllable("nghười"))

        assertTrue(VietnameseOrthography.isSyllable("nghiêng"))
        assertTrue(VietnameseOrthography.isSyllable("nghe"))
        assertTrue(VietnameseOrthography.isSyllable("người"))
        assertFalse(VietnameseOrthography.isSyllable("ngiêng"))
        assertFalse(VietnameseOrthography.isSyllable("nghười"))
    }
}

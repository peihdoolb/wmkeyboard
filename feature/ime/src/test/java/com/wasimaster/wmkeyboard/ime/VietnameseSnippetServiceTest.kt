package com.wasimaster.wmkeyboard.ime

import com.wasimaster.wmkeyboard.core.layout.Key
import com.wasimaster.wmkeyboard.core.layout.KeyAction
import com.wasimaster.wmkeyboard.core.script.ComposerType
import com.wasimaster.wmkeyboard.core.input.composer.composerFor
import com.wasimaster.wmkeyboard.core.script.LanguageRegistry
import com.wasimaster.wmkeyboard.core.script.ScriptId
import com.wasimaster.wmkeyboard.core.script.ScriptRegistry
import com.wasimaster.wmkeyboard.core.settings.KeyboardSettings
import com.wasimaster.wmkeyboard.core.settings.VietnameseSettings
import com.wasimaster.wmkeyboard.core.snippets.Snippet
import com.wasimaster.wmkeyboard.core.snippets.SnippetStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VietnameseSnippetServiceTest {

    private fun keyboardOn(
        editor: RecordingEditor,
        configureSnippets: (SnippetStore) -> Unit = {},
    ): GlideKeyboard {
        val service = GlideKeyboard(editor)
        plantPersonalStores(service)
        val snippetStoreField = WMKeyboardService::class.java.getDeclaredField("snippetStore")
        snippetStoreField.isAccessible = true
        val snippetStore = snippetStoreField.get(service) as SnippetStore
        configureSnippets(snippetStore)

        val latin = ScriptRegistry[ScriptId.LATIN]
        seedState(
            service,
            glideReadyState(
                settings = KeyboardSettings(
                    learnFromTyping = false,
                    vietnamese = VietnameseSettings(),
                ),
                fieldNoSuggestions = false,
            ).copy(
                language = LanguageRegistry.byId("vi"),
                script = latin,
                composer = composerFor(latin, ComposerType.TELEX),
            ),
        )
        return service
    }

    private fun type(service: GlideKeyboard, keys: String) {
        for (key in keys) service.onKey(Key(label = key.toString()))
    }

    private fun space(service: GlideKeyboard) = service.onKey(Key(label = "space", action = KeyAction.Space))

    @Test
    fun `plain snippet trigger expands in vietnamese telex`() {
        val editor = RecordingEditor()
        val service = keyboardOn(editor) { store ->
            store.add(label = "dc", text = "được", trigger = "dc")
            store.add(label = "ko", text = "không", trigger = "ko")
        }

        type(service, "dc")
        space(service)
        assertEquals("được ", editor.text.toString())

        type(service, "ko")
        space(service)
        assertEquals("được không ", editor.text.toString())
    }

    @Test
    fun `prefix trigger expands in vietnamese telex`() {
        val editor = RecordingEditor()
        val service = keyboardOn(editor) { store ->
            store.add(label = "shrug", text = "¯\\_(ツ)_/¯", trigger = ":shrug")
        }

        type(service, ":shrug")
        space(service)

        assertEquals("¯\\_(ツ)_/¯ ", editor.text.toString())
    }

    @Test
    fun `confirm snippet offer shows chip in vietnamese telex and tapping accepts it`() {
        val editor = RecordingEditor()
        val service = keyboardOn(editor) { store ->
            store.add(
                Snippet(
                    id = 1,
                    label = "được",
                    text = "được",
                    trigger = "dc",
                    confirm = true,
                )
            )
        }

        type(service, "dc")
        val offers = service.uiState.value.snippetOffers
        assertNotNull("Snippet offer chip should be displayed", offers)
        assertEquals("được", offers?.chips?.firstOrNull()?.text)

        service.onSnippetOfferPick(0)
        assertEquals("được", editor.text.toString())
    }
}

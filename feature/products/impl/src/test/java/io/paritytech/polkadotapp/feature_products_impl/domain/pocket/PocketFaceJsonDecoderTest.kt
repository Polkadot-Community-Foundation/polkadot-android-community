package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.model.JsAlignment
import io.paritytech.polkadotapp.feature_products_api.model.JsArrangement
import io.paritytech.polkadotapp.feature_products_api.model.JsColor
import io.paritytech.polkadotapp.feature_products_api.model.JsModifier
import io.paritytech.polkadotapp.feature_products_api.model.JsShape
import io.paritytech.polkadotapp.feature_products_api.model.JsTypographyStyle
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketFaceJsonDecoderTest {
    private val decoder = PocketFaceJsonDecoder()

    @Test
    fun `decodes the generated TypeScript shape into the shared widget vocabulary`() {
        val face = decoder.decode(
            """
            {"tag":"Column","value":{
              "modifiers":[
                {"tag":"FillWidth","value":{"enabled":true}},
                {"tag":"Padding","value":{"top":16,"end":8}},
                {"tag":"Background","value":{"color":"BgSurfaceContainer","shape":{"tag":"Rounded","value":{"radius":12}}}},
                {"tag":"Height","value":{"height":200}}
              ],
              "props":{"verticalArrangement":"SpaceBetween"},
              "children":[
                {"tag":"Text","value":{"modifiers":[],"props":{"style":"HeadlineLarge","color":"FgPrimary"},
                  "children":[{"tag":"String","value":{"text":"Loyalty"}}]}},
                {"tag":"Box","value":{"modifiers":[],"props":{"contentAlignment":"CenterEnd"},"children":[{"tag":"Nil"}]}},
                {"tag":"Button","value":{"modifiers":[],"props":{"text":"Open","variant":"Text","clickAction":"open"},"children":[]}}
              ]
            }}
            """.trimIndent()
        ).getOrThrow()

        val column = face as JsWidget.Column
        assertEquals(JsArrangement.SPACE_BETWEEN, column.verticalArrangement)
        assertEquals(
            listOf(
                JsModifier.FillMaxWidth(),
                JsModifier.Padding(top = 16, end = 8, bottom = 16, start = 8),
                JsModifier.Background(JsColor.BG_SURFACE_CONTAINER, JsShape.Rounded(12)),
                JsModifier.Size(width = null, height = 200, minWidth = null, minHeight = null),
            ),
            column.modifiers,
        )

        val title = column.children[0] as JsWidget.Text
        assertEquals("Loyalty", title.text)
        assertEquals(JsTypographyStyle.HEADLINE_LARGE, title.style)
        assertEquals(JsColor.FG_PRIMARY, title.color)

        val box = column.children[1] as JsWidget.Box
        assertEquals(JsAlignment.CENTER_END, box.contentAlignment)
        assertTrue("Nil children draw nothing", box.children.isEmpty())

        val button = column.children[2] as JsWidget.Button
        assertEquals("open", button.onClick)
    }

    @Test
    fun `rejects a tree deeper than the host bound, so a hostile preview cannot blow the stack`() {
        val nested = (1..40).fold("""{"tag":"Nil"}""") { inner, _ ->
            """{"tag":"Box","value":{"modifiers":[],"props":{},"children":[$inner]}}"""
        }

        val failure = decoder.decode(nested).exceptionOrNull()

        assertTrue(failure?.message.orEmpty().contains("deeper than 32"))
    }

    @Test
    fun `a tree exactly at the bound still decodes`() {
        val nested = (1..31).fold("""{"tag":"Nil"}""") { inner, _ ->
            """{"tag":"Box","value":{"modifiers":[],"props":{},"children":[$inner]}}"""
        }

        assertTrue(decoder.decode(nested).isSuccess)
    }

    @Test
    fun `unknown nodes, modifiers and enum names are rejected rather than drawn as something else`() {
        assertTrue(decoder.decode("""{"tag":"Image","value":{}}""").isFailure)
        assertTrue(decoder.decode("""{"tag":"Spacer","value":{"modifiers":[{"tag":"Opacity","value":{}}]}}""").isFailure)
        assertTrue(decoder.decode("""{"tag":"Text","value":{"props":{"color":"Purple"},"children":[]}}""").isFailure)
        assertTrue(decoder.decode("not json").isFailure)
    }
}

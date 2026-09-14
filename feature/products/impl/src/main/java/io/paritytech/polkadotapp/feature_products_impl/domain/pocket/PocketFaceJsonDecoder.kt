package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleArrangement
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleBackground
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleBorderStyle
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleBoxProps
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleButtonProps
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleButtonVariant
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleColorToken
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleColumnProps
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleContentAlignment
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleDimensions
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleHorizontalAlignment
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleModifier
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleRowProps
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleShape
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleTextFieldProps
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleTextProps
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleTypographyStyle
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleVerticalAlignment
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.ScaleWidget
import io.paritytech.polkadotapp.feature_products_impl.domain.serialization.scale.toJsWidget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigInteger
import javax.inject.Inject

/**
 * Decodes a face tree written in the generated TypeScript shape of the renderer vocabulary
 * (`{ tag, value }` per node, PascalCase enum names) into the same SCALE model the chat path
 * decodes, so both surfaces draw from one vocabulary. Trees deeper than [MAX_DEPTH] are rejected.
 */
class PocketFaceJsonDecoder @Inject constructor() {
    fun decode(faceJson: String): Result<JsWidget> = runCatching {
        Json.parseToJsonElement(faceJson).toNode(depth = 1).toJsWidget()
    }

    private fun JsonElement.toNode(depth: Int): ScaleWidget {
        require(depth <= MAX_DEPTH) { "face tree deeper than $MAX_DEPTH levels" }
        val node = jsonObject
        val value = node.variantValue()
        return when (val tag = node.tag()) {
            "Nil" -> ScaleWidget.Nil
            "String" -> ScaleWidget.StringNode(value.string("text"))
            "Box" -> ScaleWidget.Box(
                modifiers = value.modifiers(),
                props = ScaleBoxProps(value.props().enumOrNull<ScaleContentAlignment>("contentAlignment")),
                children = value.children(depth),
            )
            "Column" -> ScaleWidget.Column(
                modifiers = value.modifiers(),
                props = value.props().let {
                    ScaleColumnProps(
                        horizontalAlignment = it.enumOrNull<ScaleHorizontalAlignment>("horizontalAlignment"),
                        verticalArrangement = it.enumOrNull<ScaleArrangement>("verticalArrangement"),
                    )
                },
                children = value.children(depth),
            )
            "Row" -> ScaleWidget.Row(
                modifiers = value.modifiers(),
                props = value.props().let {
                    ScaleRowProps(
                        verticalAlignment = it.enumOrNull<ScaleVerticalAlignment>("verticalAlignment"),
                        horizontalArrangement = it.enumOrNull<ScaleArrangement>("horizontalArrangement"),
                    )
                },
                children = value.children(depth),
            )
            "Spacer" -> ScaleWidget.Spacer(modifiers = value.modifiers(), children = value.children(depth))
            "Text" -> ScaleWidget.Text(
                modifiers = value.modifiers(),
                props = value.props().let {
                    ScaleTextProps(
                        style = it.enumOrNull<ScaleTypographyStyle>("style"),
                        color = it.enumOrNull<ScaleColorToken>("color"),
                    )
                },
                children = value.children(depth),
            )
            "Button" -> ScaleWidget.Button(
                modifiers = value.modifiers(),
                props = value.props().let {
                    ScaleButtonProps(
                        text = it.string("text"),
                        variant = it.enumOrNull<ScaleButtonVariant>("variant"),
                        enabled = it.booleanOrNull("enabled"),
                        loading = it.booleanOrNull("loading"),
                        clickAction = it.stringOrNull("clickAction"),
                    )
                },
                children = value.children(depth),
            )
            "TextField" -> ScaleWidget.TextField(
                modifiers = value.modifiers(),
                props = value.props().let {
                    ScaleTextFieldProps(
                        text = it.string("text"),
                        placeholder = it.stringOrNull("placeholder"),
                        label = it.stringOrNull("label"),
                        enabled = it.booleanOrNull("enabled"),
                        valueChangeAction = it.stringOrNull("valueChangeAction"),
                    )
                },
                children = value.children(depth),
            )
            else -> throw IllegalArgumentException("unknown face node '$tag'")
        }
    }

    private fun JsonObject.children(depth: Int): List<ScaleWidget> =
        this["children"]?.jsonArray?.map { it.toNode(depth + 1) }.orEmpty()

    private fun JsonObject.modifiers(): List<ScaleModifier> =
        this["modifiers"]?.jsonArray?.map { it.toModifier() }.orEmpty()

    private fun JsonElement.toModifier(): ScaleModifier {
        val modifier = jsonObject
        val value = modifier.variantValue()
        return when (val tag = modifier.tag()) {
            "Margin" -> ScaleModifier.Margin(value.dimensions())
            "Padding" -> ScaleModifier.Padding(value.dimensions())
            "Background" -> ScaleModifier.Background(
                ScaleBackground(color = value.enum<ScaleColorToken>("color"), shape = value.shapeOrNull()),
            )
            "Border" -> ScaleModifier.Border(
                ScaleBorderStyle(
                    width = value.size("width"),
                    color = value.enum<ScaleColorToken>("color"),
                    shape = value.shapeOrNull(),
                ),
            )
            "Height" -> ScaleModifier.Height(value.size("height"))
            "Width" -> ScaleModifier.Width(value.size("width"))
            "MinWidth" -> ScaleModifier.MinWidth(value.size("width"))
            "MinHeight" -> ScaleModifier.MinHeight(value.size("height"))
            "FillWidth" -> ScaleModifier.FillWidth(value.boolean("enabled"))
            "FillHeight" -> ScaleModifier.FillHeight(value.boolean("enabled"))
            else -> throw IllegalArgumentException("unknown face modifier '$tag'")
        }
    }

    private fun JsonObject.shapeOrNull(): ScaleShape? {
        val shape = present("shape")?.jsonObject ?: return null
        return when (val tag = shape.tag()) {
            "Rounded" -> ScaleShape.Rounded(shape.variantValue().size("radius"))
            "Circle" -> ScaleShape.Circle
            else -> throw IllegalArgumentException("unknown face shape '$tag'")
        }
    }

    private fun JsonObject.dimensions() = ScaleDimensions(
        first = size("top"),
        second = size("end"),
        third = present("bottom")?.toSize(),
        fourth = present("start")?.toSize(),
    )

    private fun JsonObject.tag(): String = string("tag")

    // Unit variants carry no `value`; every field read off an absent one then fails as missing.
    private fun JsonObject.variantValue(): JsonObject = present("value")?.jsonObject ?: JsonObject(emptyMap())

    private fun JsonObject.props(): JsonObject = present("props")?.jsonObject ?: JsonObject(emptyMap())

    private fun JsonObject.present(key: String): JsonElement? = this[key]?.takeUnless { it is JsonNull }

    private fun JsonObject.required(key: String): JsonElement =
        requireNotNull(present(key)) { "face node missing '$key'" }

    private fun JsonObject.string(key: String): String = required(key).jsonPrimitive.content

    private fun JsonObject.stringOrNull(key: String): String? = present(key)?.jsonPrimitive?.content

    private fun JsonObject.boolean(key: String): Boolean = required(key).jsonPrimitive.content.toBooleanStrict()

    private fun JsonObject.booleanOrNull(key: String): Boolean? = present(key)?.jsonPrimitive?.content?.toBooleanStrict()

    private fun JsonObject.size(key: String): BigInteger = required(key).toSize()

    private fun JsonElement.toSize(): BigInteger = jsonPrimitive.content.toBigDecimal().toBigIntegerExact()

    private inline fun <reified E : Enum<E>> JsonObject.enum(key: String): E = required(key).toEnum()

    private inline fun <reified E : Enum<E>> JsonObject.enumOrNull(key: String): E? = present(key)?.let { it.toEnum<E>() }

    // "BgSurfaceMain" names the Kotlin constant BG_SURFACE_MAIN.
    private inline fun <reified E : Enum<E>> JsonElement.toEnum(): E =
        enumValueOf<E>(jsonPrimitive.content.replace(WORD_BOUNDARY, "_").uppercase())

    private companion object {
        const val MAX_DEPTH = 32
        val WORD_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")
    }
}

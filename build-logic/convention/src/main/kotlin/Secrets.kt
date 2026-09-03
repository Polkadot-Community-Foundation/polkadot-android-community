import com.android.build.api.dsl.VariantDimension
import java.util.Properties

fun VariantDimension.buildConfigString(name: String, value: String) {
    val escapedValue = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    buildConfigField("String", name, "\"$escapedValue\"")
}

fun Properties.readSecretOrDefault(secretName: String, default: String): String {
    return readSecretOrNull(secretName) ?: default
}

fun Properties.readSecretOrNull(secretName: String): String? {
    val secret = getProperty(secretName) ?: System.getenv(secretName)
    return secret?.takeIf { it.isNotEmpty() }
}

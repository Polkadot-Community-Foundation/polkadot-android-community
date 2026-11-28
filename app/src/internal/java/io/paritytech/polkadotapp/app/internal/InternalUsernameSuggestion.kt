package io.paritytech.polkadotapp.app.internal

import io.paritytech.polkadotapp.feature_usernames_api.presentation.MIN_USERNAME_LENGTH
import kotlin.random.Random

/** Two random English words run together, e.g. "zestyoasis": throwaway names for testers. */
object InternalUsernameSuggestion {
    internal val adjectives = listOf(
        "amber", "azure", "balmy", "bold", "brisk", "bright", "calm", "candid", "cheery", "chilly", "civic",
        "clever", "cosmic", "crisp", "curly", "dapper", "daring", "dewy", "dusky", "eager", "early", "easy",
        "fancy", "fizzy", "fluffy", "frosty", "gentle", "giddy", "glossy", "golden", "grassy", "hazy",
        "hearty", "humble", "icy", "jolly", "keen", "kind", "lanky", "lively", "lofty", "lucky", "lunar",
        "mellow", "merry", "misty", "mossy", "nimble", "noble", "olive", "opal", "plucky", "polar", "proud",
        "quaint", "quick", "quiet", "rapid", "rosy", "rustic", "sandy", "shiny", "silky", "sleepy", "snowy",
        "solar", "spicy", "steady", "stormy", "sunny", "swift", "tangy", "tidy", "tranquil", "vivid",
        "warm", "wavy", "windy", "witty", "woolly", "zesty", "breezy", "cozy", "dreamy", "fuzzy", "happy",
        "jazzy", "leafy", "minty", "peachy", "perky", "rainy", "sly", "snug", "sturdy", "velvet", "wild"
    )

    internal val nouns = listOf(
        "acorn", "anchor", "apple", "badger", "basin", "beacon", "birch", "bison", "bloom", "breeze",
        "brook", "cactus", "canyon", "cedar", "cherry", "cliff", "clover", "comet", "coral", "cove",
        "crane", "creek", "dahlia", "delta", "dune", "eagle", "ember", "falcon", "fern", "fjord", "finch",
        "forest", "garnet", "glacier", "grove", "harbor", "hazel", "heron", "hollow", "island", "ivy",
        "jasper", "juniper", "kettle", "lagoon", "lantern", "lark", "lemon", "lotus", "maple", "meadow",
        "mesa", "meteor", "mint", "moose", "nectar", "oasis", "ocean", "orchid", "otter", "owl", "panda",
        "pebble", "pepper", "pine", "planet", "plum", "pond", "poppy", "quartz", "quill", "raven", "reef",
        "ridge", "river", "robin", "saffron", "sage", "salmon", "spruce", "summit", "thistle", "thunder",
        "tulip", "tundra", "valley", "walnut", "willow", "wren", "yarrow", "zephyr", "bramble", "cobble",
        "dingo", "gecko", "lynx", "marlin", "newt", "puffin", "tapir", "walrus"
    )

    internal val forbiddenTerms = listOf(
        "test", "demo", "admin", "polkadot", "parity", "pcf", "official", "support", "staff", "team", "dev",
        "user", "bot", "fake", "real", "root", "mod"
    )

    fun next(random: Random = Random.Default): String =
        generateSequence { adjectives.random(random) + nouns.random(random) }
            .first(::isAcceptable)

    internal fun isAcceptable(name: String): Boolean =
        name.length >= MIN_USERNAME_LENGTH &&
            name.all { it in 'a'..'z' } &&
            forbiddenTerms.none { it in name }
}

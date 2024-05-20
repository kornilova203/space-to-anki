package kornilova

import space.jetbrains.api.runtime.types.TD_MemberProfile
import java.util.*

enum class LocationType(val ids: List<String>) {
    BUILDING(listOf("Building")),
    CITY(listOf("City", "Campus")),
    REGION(listOf("Region")),
}

private val underscores = Regex("_+")

class Location(private val parts: List<Pair<LocationType, String>>) {
    val presentableName: String
        get() = (parts.filter { it.first == LocationType.REGION }
            .firstNotNullOfOrNull { it.second.getCountryFlag() }
            ?.plus(" ") ?: "") +
                parts.joinToString { it.second }

    val tags: List<String>
        get() = parts.map {
            it.second.lowercase(Locale.ENGLISH)
                .replace(" ", "_")
                .replace("-", "_")
                .replace(underscores, "_")
        }
}

private fun String.getCountryFlag(): String? {
    return when (this) {
        "Armenia" -> "\uD83C\uDDE6\uD83C\uDDF2"
        "Cyprus" -> "\uD83C\uDDE8\uD83C\uDDFE"
        "Czech Republic" -> "\uD83C\uDDE8\uD83C\uDDFF"
        "Germany" -> "\uD83C\uDDE9\uD83C\uDDEA"
        "Netherlands" -> "\uD83C\uDDF3\uD83C\uDDF1"
        "Sweden" -> "\uD83C\uDDF8\uD83C\uDDEA"
        "Serbia" -> "\uD83C\uDDF7\uD83C\uDDF8"
        "England" -> "\uD83C\uDDEC\uD83C\uDDE7"
        else -> null
    }
}


fun extractLocations(profile: TD_MemberProfile): List<Location> {
    return profile.locations.mapNotNull { leafLocation ->
        val parts = generateSequence(leafLocation.location) { l -> l.parent }
            .mapNotNull { l ->
                val type = LocationType.entries.find { it.ids.contains(l.type) }
                if (type == null) null
                else type to l.name
            }.toList()
        if (parts.isEmpty()) null
        else Location(parts)
    }
}

package kornilova

import kornilova.SimpleLocation.entries
import space.jetbrains.api.runtime.types.TD_Location
import space.jetbrains.api.runtime.types.TD_MemberProfile

enum class SimpleLocation(val id: String, val tag: String, val presentableName: String) {
    BERLIN("1VSTug1k3zI8", "Berlin", "\uD83C\uDDE9\uD83C\uDDEA Berlin"),
    NETHERLANDS("3fkpd53c1Sls", "Netherlands", "\uD83C\uDDF3\uD83C\uDDF1 Netherlands"),
    MUNICH("1L51AV11kGh1", "Munich", "\uD83C\uDDE9\uD83C\uDDEA Munich"),
    PRAGUE("3FYOKl3LmPsO", "Prague", "\uD83C\uDDE8\uD83C\uDDFF Prague"),
}

fun extractSimpleLocation(profile: TD_MemberProfile): SimpleLocation? {
    for (location in profile.locations) {
        var l: TD_Location? = location.location
        while (l != null) {
            val simpleLocation = findById(l.id)
            if (simpleLocation != null) {
                return simpleLocation
            }
            l = l.parent
        }
    }
    return null
}

fun extractLocationTags(profile: TD_MemberProfile): Set<String> {
    val simpleLocation = extractSimpleLocation(profile)
    return if (simpleLocation != null) setOf(simpleLocation.tag) else emptySet()
}

fun findById(id: String): SimpleLocation? {
    return entries.find { it.id == id }
}

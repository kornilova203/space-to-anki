package kornilova

import kornilova.SimpleLocation.entries
import space.jetbrains.api.runtime.types.TD_Location
import space.jetbrains.api.runtime.types.TD_MemberProfile

enum class SimpleLocation(val id: String, val presentableName: String) {
    BERLIN("1VSTug1k3zI8", "Berlin"),
    NETHERLANDS("3fkpd53c1Sls", "Netherlands"),
    MUNICH("1L51AV11kGh1", "Munich"),
    PRAGUE("3FYOKl3LmPsO", "Prague"),
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
    return if (simpleLocation != null) setOf(simpleLocation.presentableName) else emptySet()
}

fun findById(id: String): SimpleLocation? {
    return entries.find { it.id == id }
}

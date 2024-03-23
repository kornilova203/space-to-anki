package kornilova

import space.jetbrains.api.runtime.types.TD_Location
import space.jetbrains.api.runtime.types.TD_MemberProfile

val berlinId = "1VSTug1k3zI8"
val netherlandsId = "3fkpd53c1Sls"
val munichId = "1L51AV11kGh1"
val pragueId = "3FYOKl3LmPsO"

val locationTags = mapOf(
    berlinId to "Berlin",
    netherlandsId to "Netherlands",
    munichId to "Munich",
    pragueId to "Prague",
)

fun extractLocationTags(profile: TD_MemberProfile): Set<String> {
    if (profile.locations.size > 1) {
        throw IllegalStateException("Unexpected number of locations: ${profile.locations}")
    }
    if (profile.locations.isEmpty()) {
        return emptySet()
    }
    var location: TD_Location? = profile.locations[0].location
    while (location != null) {
        val tag = locationTags[location.id]
        if (tag != null) {
            return setOf(tag)
        }
        location = location.parent
    }
    return emptySet()
}
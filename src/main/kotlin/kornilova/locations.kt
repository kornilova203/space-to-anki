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
    for (location in profile.locations) {
        var l: TD_Location? = location.location
        while (l != null) {
            val tag = locationTags[l.id]
            if (tag != null) {
                return setOf(tag)
            }
            l = l.parent
        }
    }
    return emptySet()
}
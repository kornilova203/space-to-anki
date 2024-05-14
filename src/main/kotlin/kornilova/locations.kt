package kornilova

import kornilova.SimpleLocation.entries
import space.jetbrains.api.runtime.types.TD_Location
import space.jetbrains.api.runtime.types.TD_MemberProfile

enum class SimpleLocation(val id: String, val tag: String, val presentableName: String) {
    BERLIN("1VSTug1k3zI8", "Berlin", "\uD83C\uDDE9\uD83C\uDDEA Berlin"),
    NETHERLANDS("3fkpd53c1Sls", "Netherlands", "\uD83C\uDDF3\uD83C\uDDF1 Netherlands"),
    MUNICH("1L51AV11kGh1", "Munich", "\uD83C\uDDE9\uD83C\uDDEA Munich"),
    PRAGUE("3FYOKl3LmPsO", "Prague", "\uD83C\uDDE8\uD83C\uDDFF Prague"),
    LIMASSOL("3D2sOm4d9Ncn", "Limassol", "\uD83C\uDDE8\uD83C\uDDFE Limassol"),
    PAPHOS("2O8be34YBK5t", "Paphos", "\uD83C\uDDE8\uD83C\uDDFE Paphos"),
    NICOSIA("1UHIxV1itE7x", "Nicosia", "\uD83C\uDDE8\uD83C\uDDFE Nicosia"),
    LARNACA("30TuuV2j3VIT", "Larnaca", "\uD83C\uDDE8\uD83C\uDDFE Larnaca"),
    ARMENIA("2sgqQq34Lvfd", "Armenia", "\uD83C\uDDE6\uD83C\uDDF2 Armenia"),
    LONDON("2W9t4C2cQEQi", "London", "\uD83C\uDDEC\uD83C\uDDE7 London"),
    BELGRADE("3PjPdT40jjxb", "Belgrade", "\uD83C\uDDF7\uD83C\uDDF8 Belgrade"),
    SWEDEN("P3TmV0Q8Rqu", "Sweden", "\uD83C\uDDF8\uD83C\uDDEA Sweden")
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

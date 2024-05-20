package kornilova

import kotlinx.datetime.LocalDate


class Colleague(
    val id: String,
    val firstName: String,
    val lastName: String,
    val allNameSpellings: List<Pair<String, String>>,
    val profilePictureId: String,
    val memberships: List<Membership>,
    val locations: List<Location>,
    val startDate: LocalDate,
    val tags: Set<String>,
)

class ColleagueWithPicture(val colleague: Colleague, val picture: ByteArray?)

class Membership(
    val role: String,
    val team: String,
    val lead: Boolean,
    val ratio: Float
)

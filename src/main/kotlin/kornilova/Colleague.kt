package kornilova

class Colleague(
    val id: String,
    val firstName: String,
    val lastName: String,
    val profilePictureId: String,
    val memberships: List<Membership>,
    val location: String?,
    val tags: Set<String>,
)

class ColleagueWithPicture(val colleague: Colleague, val picture: ByteArray)

class Membership(
    val role: String,
    val team: String,
    val lead: Boolean,
    val ratio: Float
)

package kornilova

class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val profilePictureId: String,
    val image: ByteArray,
    val memberships: List<Membership>,
    val tags: Set<String>,
)

class Membership(
    val role: String,
    val team: String,
    val lead: Boolean,
    val ratio: Float
)

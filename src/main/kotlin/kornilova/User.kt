package kornilova

class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val profilePictureId: String,
    val memberships: List<Membership>,
    val tags: Set<String>,
)

class UserWithPicture(val user: User, val picture: ByteArray)

class Membership(
    val role: String,
    val team: String,
    val lead: Boolean,
    val ratio: Float
)

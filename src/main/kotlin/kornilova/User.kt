package kornilova

class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val profilePictureId: String,
    val image: ByteArray,
    val memberships: List<Membership>
)

class Membership(val role: String, val team: String)
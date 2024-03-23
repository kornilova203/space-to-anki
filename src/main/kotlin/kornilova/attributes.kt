package kornilova

val userAttributes = listOf(FirstName, LastName, Picture)

sealed interface UserAttribute {
    fun get(user: User): String
}

data object FirstName : UserAttribute {
    override fun get(user: User): String = user.firstName
}

data object LastName : UserAttribute {
    override fun get(user: User): String = user.lastName
}


data object Picture : UserAttribute {
    override fun get(user: User): String {
        return "<img src='${user.profilePictureId}.jpg'>"
    }
}
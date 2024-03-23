package kornilova

// id has to be first because Anki uses first field as an id when re-importing notes with the update option turned on
val userAttributes = listOf(UserId, FirstName, LastName, Picture)

sealed interface UserAttribute {
    fun get(user: User): String
}

data object UserId : UserAttribute {
    override fun get(user: User): String = user.id
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
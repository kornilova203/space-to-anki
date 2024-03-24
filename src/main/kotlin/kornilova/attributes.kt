package kornilova

// id has to be first because Anki uses first field as an id when re-importing notes with the update option turned on
val colleagueAttributes = listOf(ColleagueId, FirstName, LastName, Picture, Memberships, Location)

sealed interface ColleagueAttribute {
    fun get(colleague: Colleague): String
}

data object ColleagueId : ColleagueAttribute {
    override fun get(colleague: Colleague): String = colleague.id
}

data object FirstName : ColleagueAttribute {
    override fun get(colleague: Colleague): String = colleague.firstName
}

data object LastName : ColleagueAttribute {
    override fun get(colleague: Colleague): String = colleague.lastName
}


data object Picture : ColleagueAttribute {
    override fun get(colleague: Colleague): String {
        return "<img src='${colleague.profilePictureId}.jpg'>"
    }
}

data object Memberships : ColleagueAttribute {
    override fun get(colleague: Colleague): String {
        return colleague.memberships.joinToString("<br/>") {
            "${it.role} at ${it.team}"
        }
    }
}

data object Location : ColleagueAttribute {
    override fun get(colleague: Colleague): String = colleague.location ?: ""
}

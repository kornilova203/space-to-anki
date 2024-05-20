package kornilova

import java.awt.Color

// id has to be first because Anki uses first field as an id when re-importing notes with the update option turned on
val colleagueAttributes = listOf(
    ColleagueId,
    ColoredFirstCharacter(FirstName),
    ColoredFirstCharacter(LastName),
    AllNameSpellings,
    Picture,
    Memberships,
    LocationAttr,
    StartDate,
)

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

data object AllNameSpellings : ColleagueAttribute {
    override fun get(colleague: Colleague): String = colleague.allNameSpellings.joinToString { "${it.first} ${it.second}" }
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

data object LocationAttr : ColleagueAttribute {
    override fun get(colleague: Colleague): String = colleague.locations.joinToString("\n") { it.presentableName }
}

data object StartDate : ColleagueAttribute {
    override fun get(colleague: Colleague): String {
        val bucket = chooseStartTimeBucket(colleague.startDate)
        val text = colleague.startDate.toString()
        return if (bucket.color != null) text.bold().color(bucket.color) else text
    }
}

data class ColoredFirstCharacter(val attribute: ColleagueAttribute) : ColleagueAttribute {
    override fun get(colleague: Colleague): String {
        val text = attribute.get(colleague)

        val color = chooseColor(text[0])
        return if (color != null) text.substring(0, 1).bold().color(color) + text.substring(1)
        else text
    }
}

fun String.bold(): String = "<b>$this</b>"

fun String.color(color: Color): String {
    return "<span style='color: rgb(${color.red}, ${color.green}, ${color.blue});'>$this</span>"
}

private fun chooseColor(c: Char): Color? {
    return when (c) {
        'А', 'И' -> Color(255, 0, 0)
        'Б', 'Г', 'О', 'У' -> Color(60, 60, 255)
        'К', 'П', 'Т', 'Ф', 'Х' -> Color(160, 160, 160)
        'Л', 'М', 'Н' -> Color(56, 186, 78)
        'С' -> Color(255, 200, 0)
        'Р', 'Я', 'Ю' -> Color(228, 45, 228)
        'Д' -> Color(112, 42, 37)
        'Е', 'Ë' -> Color(50, 42, 174)
        else -> null
    }
}
// «а», «б», «в», «г», «д», «е», «ё», «ж», «з», «и», «й», «к», «л», «м», «н», «о», «п», «р», «с», «т», «у»,
// «ф», «х», «ц», «ч», «ш», «щ», «ъ», «ы», «ь», «э», «ю», «я»
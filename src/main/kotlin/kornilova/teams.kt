package kornilova

import space.jetbrains.api.runtime.types.TD_MemberProfile
import space.jetbrains.api.runtime.types.TD_Team

enum class Team(val id: String, val tag: String) {
    CORE("auzA90h98AC", "core-team")
}

fun extractTeamTags(profile: TD_MemberProfile): Set<String> {
    val tags = mutableSetOf<String>()
    for (membership in profile.memberships) {
        var team: TD_Team? = membership.team
        while (team != null) {
            Team.entries.find { it.id == team!!.id }?.let {
                tags.add(it.tag)
            }
            team = team.parent
        }
    }
    return tags
}

package kornilova

import org.example.kornilova.MyBatch
import space.jetbrains.api.runtime.BatchInfo
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.resources.teamDirectory
import space.jetbrains.api.runtime.types.TD_MemberProfile
import space.jetbrains.api.runtime.types.partials.TD_MemberProfilePartial

sealed interface Scope {
    suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: BatchInfo,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile>
}

abstract class LocationScope(private val locationId: String) : Scope {
    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: BatchInfo,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile> {
        val batch = client.teamDirectory.profiles.getAllProfiles(
            locationId = locationId,
            batchInfo = batchInfo,
            buildPartial = buildPartial
        )
        return MyBatch(batch.data, batch.next)
    }
}

object Berlin : LocationScope("1VSTug1k3zI8")

class EmailScope(private val email: String) : Scope {
    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: BatchInfo,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile> {
        val profile = client.teamDirectory.profiles.getProfileByEmail(email, buildPartial = buildPartial)
        return MyBatch(listOf(profile), null)
    }
}

package kornilova

import space.jetbrains.api.runtime.Batch
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
    ): Batch<TD_MemberProfile>
}

abstract class LocationScope(private val locationId: String) : Scope {
    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: BatchInfo,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): Batch<TD_MemberProfile> {
        return client.teamDirectory.profiles.getAllProfiles(
            locationId = locationId,
            batchInfo = batchInfo,
            buildPartial = buildPartial
        )
    }
}

object Berlin : LocationScope("1VSTug1k3zI8")

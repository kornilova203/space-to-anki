package kornilova

import space.jetbrains.api.runtime.BatchInfo
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.resources.teamDirectory
import space.jetbrains.api.runtime.types.TD_MemberProfile
import space.jetbrains.api.runtime.types.partials.TD_MemberProfilePartial

private const val batchSize = 10

sealed interface Scope<B : MyBatchInfo<TD_MemberProfile>> {
    val initialBatchInfo: B

    suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: B,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile, B>
}

abstract class LocationScope(private val locationId: String) : Scope<StandardBatchInfo<TD_MemberProfile>> {
    override val initialBatchInfo = StandardBatchInfo<TD_MemberProfile>(BatchInfo("0", batchSize), true)

    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: StandardBatchInfo<TD_MemberProfile>,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile, StandardBatchInfo<TD_MemberProfile>> {
        val batch = client.teamDirectory.profiles.getAllProfiles(
            locationId = locationId,
            batchInfo = batchInfo.batchInfo,
            buildPartial = buildPartial
        )
        return MyBatch(StandardBatchInfo(BatchInfo(batch.next, batchSize), batch.data.isNotEmpty()), batch.data)
    }
}

object Berlin : LocationScope(berlinId)
object Netherlands : LocationScope(netherlandsId)

class EmailScope(private val email: String) : Scope<SimpleBatchInfo<TD_MemberProfile>> {
    override val initialBatchInfo = SimpleBatchInfo<TD_MemberProfile>(true)

    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: SimpleBatchInfo<TD_MemberProfile>,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile, SimpleBatchInfo<TD_MemberProfile>> {
        val profile = client.teamDirectory.profiles.getProfileByEmail(email, buildPartial = buildPartial)
        return MyBatch(SimpleBatchInfo(false), listOf(profile))
    }
}

class EmailsScope(private val emails: List<String>) : Scope<ListBatchInfo<TD_MemberProfile, String>> {
    override val initialBatchInfo: ListBatchInfo<TD_MemberProfile, String>
        get() = ListBatchInfo(emails)

    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: ListBatchInfo<TD_MemberProfile, String>,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile, ListBatchInfo<TD_MemberProfile, String>> {
        val profile = client.teamDirectory.profiles.getProfileByEmail(batchInfo.listTail.first(), buildPartial = buildPartial)
        return MyBatch(ListBatchInfo(batchInfo.listTail.drop(1)), listOf(profile))
    }
}

class StandardBatchInfo<T>(val batchInfo: BatchInfo, override val hasNext: Boolean) : MyBatchInfo<T>
class SimpleBatchInfo<T>(override val hasNext: Boolean) : MyBatchInfo<T>
class ListBatchInfo<T, E>(val listTail: List<E>) : MyBatchInfo<T> {
    override val hasNext: Boolean
        get() = listTail.isNotEmpty()
}
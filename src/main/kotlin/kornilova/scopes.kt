package kornilova

import kornilova.SimpleLocation.BERLIN
import kornilova.SimpleLocation.NETHERLANDS
import space.jetbrains.api.runtime.BatchInfo
import space.jetbrains.api.runtime.NotFoundException
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.resources.teamDirectory
import space.jetbrains.api.runtime.types.TD_MemberProfile
import space.jetbrains.api.runtime.types.partials.TD_MemberProfilePartial
import kotlin.Exception

private const val batchSize = 10

sealed interface Scope<B : MyBatchInfo<TD_MemberProfile>> {
    fun initialBatchInfo(skip: Int): B

    suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: B,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile, B>
}

abstract class LocationScope(private val location: SimpleLocation) : Scope<StandardBatchInfo<TD_MemberProfile>> {
    override fun initialBatchInfo(skip: Int): StandardBatchInfo<TD_MemberProfile> {
        return StandardBatchInfo(BatchInfo(skip.toString(), batchSize), true)
    }

    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: StandardBatchInfo<TD_MemberProfile>,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile, StandardBatchInfo<TD_MemberProfile>> {
        val batch = client.teamDirectory.profiles.getAllProfiles(
            locationId = location.id,
            batchInfo = batchInfo.batchInfo,
            buildPartial = buildPartial
        )
        val data = batch.data.filter { !it.notAMember }
        return MyBatch(StandardBatchInfo(BatchInfo(batch.next, batchSize), batch.data.isNotEmpty()), data)
    }
}

object Berlin : LocationScope(BERLIN)
object Netherlands : LocationScope(NETHERLANDS)

class EmailScope(private val email: String) : Scope<SimpleBatchInfo<TD_MemberProfile>> {
    override fun initialBatchInfo(skip: Int): SimpleBatchInfo<TD_MemberProfile> {
        return SimpleBatchInfo(true)
    }

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
    override fun initialBatchInfo(skip: Int): ListBatchInfo<TD_MemberProfile, String> {
        return ListBatchInfo(emails)
    }

    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: ListBatchInfo<TD_MemberProfile, String>,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile, ListBatchInfo<TD_MemberProfile, String>> {
        val email = batchInfo.listTail.first()
        try {
            val profile = client.teamDirectory.profiles.getProfileByEmail(email, buildPartial = buildPartial)
            return MyBatch(ListBatchInfo(batchInfo.listTail.drop(1)), listOf(profile))
        } catch (e: NotFoundException) {
            throw Exception("Cannot find user with email $email", e)
        }
    }
}

abstract class TeamScope(private val team: Team) : Scope<StandardBatchInfo<TD_MemberProfile>> {
    override fun initialBatchInfo(skip: Int): StandardBatchInfo<TD_MemberProfile> {
        return StandardBatchInfo(BatchInfo("0", batchSize), true)
    }

    override suspend fun getAllProfiles(
        client: SpaceClient,
        batchInfo: StandardBatchInfo<TD_MemberProfile>,
        buildPartial: TD_MemberProfilePartial.() -> Unit
    ): MyBatch<TD_MemberProfile, StandardBatchInfo<TD_MemberProfile>> {
        val batch = client.teamDirectory.profiles.getAllProfiles(teamId = team.id, batchInfo = batchInfo.batchInfo, buildPartial = buildPartial)
        val data = batch.data.filter { !it.notAMember }
        return MyBatch(StandardBatchInfo(BatchInfo(batch.next, batchSize), batch.data.isNotEmpty() && batch.next.isNotEmpty()), data)
    }
}

object CoreTeam : TeamScope(Team.CORE)

class StandardBatchInfo<T>(val batchInfo: BatchInfo, override val hasNext: Boolean) : MyBatchInfo<T>
class SimpleBatchInfo<T>(override val hasNext: Boolean) : MyBatchInfo<T>
class ListBatchInfo<T, E>(val listTail: List<E>) : MyBatchInfo<T> {
    override val hasNext: Boolean
        get() = listTail.isNotEmpty()
}
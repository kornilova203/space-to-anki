package kornilova

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.ktorClientForSpace
import space.jetbrains.api.runtime.types.TD_MemberProfile
import space.jetbrains.api.runtime.types.partials.TD_MemberProfilePartial
import java.io.File

fun main() {
    val scope = Berlin

    val spaceHttpClient = ktorClientForSpace()
    val token = File("src/main/resources/token.txt").readText()
    val client = SpaceClient(
        ktorClient = spaceHttpClient,
        serverUrl = "https://jetbrains.team",
        token = token
    )

    val users = runBlocking {
        fetchUsers(scope, client, token)
    }
    makeAnkiDeck(users.take(5).toList())
}

private suspend fun <B : MyBatchInfo<TD_MemberProfile>> fetchUsers(
    scope: Scope<B>,
    client: SpaceClient,
    token: String
): Sequence<User> {
    val httpClient = HttpClient()

    val profiles = fetchAll(scope.initialBatchInfo) { batchInfo ->
        val buildPartial: TD_MemberProfilePartial.() -> Unit = {
            defaultPartial()
            memberships {
                role {
                    name()
                }
                team {
                    name()
                }
            }
        }
        scope.getAllProfiles(client, batchInfo, buildPartial)
    }
    return profiles.map { profile ->
        val builder = HttpRequestBuilder()
        val profilePictureId = profile.profilePicture!!
        builder.url(Url("https://jetbrains.team/d/${profilePictureId}"))
        builder.header("Authorization", "Bearer $token")
        val image = runBlocking {
            val httpResponse = httpClient.get(builder)
            httpResponse.readBytes()
        }
        User(
            profile.id,
            profile.name.firstName,
            profile.name.lastName,
            profilePictureId,
            image,
            profile.memberships.map { Membership(it.role.name, it.team.name) }
        )
    }
}

fun makeAnkiDeck(users: List<User>) {
    val resDir = File("result")
    val imagesDir = resDir.resolve("images")
    resDir.deleteRecursively()
    imagesDir.mkdirs()

    for (user in users) {
        imagesDir.resolve("${user.profilePictureId}.jpg").writeBytes(user.image)
    }
    val rows = users.map { user ->
        userAttributes.map { attribute -> attribute.get(user) }
    }

    csvWriter().writeAll(rows, resDir.resolve("result.csv"))
}

suspend fun <T, B : MyBatchInfo<T>> fetchAll(initialBatchInfo: B, query: suspend (B) -> MyBatch<T, B>): Sequence<T> {
    return generateSequence(query(initialBatchInfo)) { batch ->
        if (!batch.batchInfo.hasNext) null
        else {
            runBlocking {
                query(batch.batchInfo)
            }
        }
    }.flatMap { batch -> batch.data }
}

interface MyBatchInfo<T> {
    val hasNext: Boolean
}

class MyBatch<T, B : MyBatchInfo<T>>(val batchInfo: B, val data: List<T>)

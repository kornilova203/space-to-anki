package org.example.kornilova

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kornilova.*
import kotlinx.coroutines.runBlocking
import space.jetbrains.api.runtime.BatchInfo
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.ktorClientForSpace
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

private suspend fun fetchUsers(
    scope: Scope,
    client: SpaceClient,
    token: String
): Sequence<User> {
    val httpClient = HttpClient()

    val profiles = fetchAll { batchInfo ->
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

private const val batchSize = 10

suspend fun <T> fetchAll(query: suspend (BatchInfo) -> MyBatch<T>): Sequence<T> {
    val batchInfo = BatchInfo("0", batchSize)
    val batch = query(batchInfo)

    return generateSequence(Pair(batchInfo, batch)) { (batchInfo, batch) ->
        if (batch.next == null) null
        else {
            val nextBatchInfo = BatchInfo(batch.next, batchSize)
            val nextBatch = runBlocking {
                query(batchInfo)
            }
            Pair(nextBatchInfo, nextBatch)
        }
    }.flatMap { (_, batch) -> batch.data }
}

class MyBatch<T>(val data: List<T>, val next: String?)

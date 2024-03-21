package org.example.kornilova

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kornilova.User
import kotlinx.coroutines.runBlocking
import space.jetbrains.api.runtime.Batch
import space.jetbrains.api.runtime.BatchInfo
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.ktorClientForSpace
import space.jetbrains.api.runtime.resources.teamDirectory
import java.io.File

const val berlin = "1VSTug1k3zI8"

fun main() {
    val spaceHttpClient = ktorClientForSpace()
    val token = File("src/main/resources/token.txt").readText()
    val client = SpaceClient(
        ktorClient = spaceHttpClient,
        serverUrl = "https://jetbrains.team",
        token = token
    )

    val httpClient = HttpClient()

    runBlocking {
        val profiles = fetchAll { batchInfo ->
            client.teamDirectory.profiles.getAllProfiles(locationId = berlin, batchInfo = batchInfo)
        }
        val users = profiles.map { profile ->
            val builder = HttpRequestBuilder()
            builder.url(Url("https://jetbrains.team/d/${profile.profilePicture!!}"))
            builder.header("Authorization", "Bearer $token")
            runBlocking {
                val httpResponse = httpClient.get(builder)
                File("${profile.name.firstName}.jpg").writeBytes(httpResponse.readBytes())
            }
            User(profile.name.firstName, profile.name.lastName)
        }
        makeAnkiDeck(users.take(5).toList())
    }
}

fun makeAnkiDeck(users: List<User>) {

}

private const val batchSize = 10

suspend fun <T> fetchAll(query: suspend (BatchInfo) -> Batch<T>): Sequence<T> {
    val batchInfo = BatchInfo("0", batchSize)
    val batch = query(batchInfo)

    return generateSequence(Pair(batchInfo, batch)) { (batchInfo, batch) ->
        val nextBatchInfo = BatchInfo(batch.next, batchSize)
        val nextBatch = runBlocking {
            query(batchInfo)
        }
        Pair(nextBatchInfo, nextBatch)
    }.flatMap { (_, batch) -> batch.data }
}

fun Batch<*>.hasNext() = data.isNotEmpty()
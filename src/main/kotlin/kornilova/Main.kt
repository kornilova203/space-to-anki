package org.example.kornilova

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
    val client = SpaceClient(
        ktorClient = spaceHttpClient,
        serverUrl = "https://jetbrains.team",
        token = File("src/main/resources/token.txt").readText()
    )


    runBlocking {
        val profiles = fetchAll { batchInfo ->
            client.teamDirectory.profiles.getAllProfiles(locationId = berlin, batchInfo = batchInfo)
        }
        val users = profiles.map { profile -> User(profile.name.firstName, profile.name.lastName) }
        makeAnkiDeck(users)
    }
}

fun makeAnkiDeck(users: List<User>) {

}

suspend fun <T> fetchAll(query: suspend (BatchInfo) -> Batch<T>): List<T> {
    val list = mutableListOf<T>()
    var batchInfo = BatchInfo("0", 100)
    do {
        val batch = query(batchInfo)

        list.addAll(batch.data)

        batchInfo = BatchInfo(batch.next, 100)
    } while (batch.hasNext())
    return list
}

fun Batch<*>.hasNext() = data.isNotEmpty()
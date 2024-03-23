package kornilova

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.ktorClientForSpace
import space.jetbrains.api.runtime.types.FractionCFValue
import space.jetbrains.api.runtime.types.TD_MemberProfile
import space.jetbrains.api.runtime.types.partials.TD_MemberProfilePartial
import java.io.File

fun main() {
    val scope = EmailsScope(File("emails.txt").readLines())
    val additionalTags = listOf<String>()

    val spaceHttpClient = ktorClientForSpace()
    val token = File("src/main/resources/token.txt").readText()
    val client = SpaceClient(
        ktorClient = spaceHttpClient,
        serverUrl = "https://jetbrains.team",
        token = token
    )

    val users = runBlocking {
        fetchUsers(scope, client, token)
    }.toList()
    tagsDir.mkdirs()
    rememberTags(users, additionalTags)
    val allTags = readAllTags()
    makeAnkiDeck(users.toList(), allTags)
}

val tagsDir = File("tags")

fun rememberTags(users: List<User>, tags: List<String>) {
    if (tags.isEmpty()) return
    val tagsFile = tagsDir.resolve("${System.currentTimeMillis()}.txt")
    csvWriter().writeAll(users.map { listOf(it.id, tags.joinToString(" ")) }, tagsFile)
}

fun readAllTags(): Map<String, Set<String>> {
    val files = tagsDir.listFiles() ?: return emptyMap()
    val tags = mutableMapOf<String, MutableSet<String>>()
    for (file in files) {
        val rows = csvReader().readAll(file)
        for (row in rows) {
            tags.computeIfAbsent(row[0]) { mutableSetOf() }.addAll(row[1].split(" "))
        }
    }
    return tags
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
            managers(this)
            locations {
                location {
                    id()
                    name()
                    type()
                    parent(this)
                }
            }
            languages {
                name()
                language {
                    name()
                }
            }
            memberships {
                lead()
                role {
                    name()
                }
                team {
                    name()
                }
                customFields()
            }
        }
        scope.getAllProfiles(client, batchInfo, buildPartial)
    }
    return profiles.mapNotNull { profile ->
        val builder = HttpRequestBuilder()
        val profilePictureId = profile.profilePicture ?: return@mapNotNull null
        builder.url(Url("https://jetbrains.team/d/${profilePictureId}"))
        builder.header("Authorization", "Bearer $token")
        val image = runBlocking {
            val httpResponse = httpClient.get(builder)
            httpResponse.readBytes()
        }
        val russianName = profile.languages.find { it.language.name == "Russian" }?.name
        User(
            profile.id,
            russianName?.firstName ?: profile.name.firstName,
            russianName?.lastName ?: profile.name.lastName,
            profilePictureId,
            image,
            profile.memberships.map {
                val ratio = (it.customFields["Ratio"] as? FractionCFValue)?.value
                Membership(it.role.name, it.team.name, it.lead, if (ratio != null) ratio.numerator.toFloat() / ratio.denominator else 1f)
            }.sortedWith(compareBy<Membership>({ it.lead }, { it.ratio }).reversed()),
            extractLocationTags(profile)
        )
    }
}

fun makeAnkiDeck(users: List<User>, additionalTags: Map<String, Set<String>>) {
    val resDir = File("result")
    val imagesDir = resDir.resolve("images")
    resDir.deleteRecursively()
    imagesDir.mkdirs()

    for (user in users) {
        imagesDir.resolve("${user.profilePictureId}.jpg").writeBytes(user.image)
    }
    val rows = users.map { user ->
        val allTags = user.tags + (additionalTags[user.id] ?: emptySet())
        userAttributes.map { attribute -> attribute.get(user) }.plus(allTags.joinToString(" "))
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

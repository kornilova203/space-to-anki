package kornilova

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.ktor.client.*
import io.ktor.client.plugins.*
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
    val scope = CoreTeam
    val additionalTags = listOf<String>()

    val spaceHttpClient = ktorClientForSpace {
        configureClient()
    }
    val token = File("src/main/resources/token.txt").readText()
    val client = SpaceClient(
        ktorClient = spaceHttpClient,
        serverUrl = "https://jetbrains.team",
        token = token
    )

    val httpClient = HttpClient {
        configureClient()
    }
    val colleagues = runBlocking {
        fetchColleagues(scope, client)
    }.map { colleague ->
        val picture = runBlocking {
            loadPicture(token, httpClient, colleague.profilePictureId)
        }
        ColleagueWithPicture(colleague, picture)
    }.toList()
    tagsDir.mkdirs()
    rememberTags(colleagues, additionalTags)
    val allTags = readAllTags()
    makeAnkiDeck(colleagues.toList(), allTags)
}

suspend fun loadPicture(token: String, httpClient: HttpClient, profilePictureId: String): ByteArray {
    val builder = HttpRequestBuilder()
    builder.url(Url("https://jetbrains.team/d/${profilePictureId}"))
    builder.header("Authorization", "Bearer $token")
    return httpClient.get(builder).readBytes()
}

val tagsDir = File("tags")

fun rememberTags(colleagues: List<ColleagueWithPicture>, tags: List<String>) {
    if (tags.isEmpty()) return
    val tagsFile = tagsDir.resolve("${System.currentTimeMillis()}.txt")
    csvWriter().writeAll(colleagues.map { listOf(it.colleague.id, tags.joinToString(" ")) }, tagsFile)
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

private suspend fun <B : MyBatchInfo<TD_MemberProfile>> fetchColleagues(
    scope: Scope<B>,
    client: SpaceClient,
    skip: Int = 0
): Sequence<Colleague> {
    val profiles = fetchAll(scope.initialBatchInfo(skip)) { batchInfo ->
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
                    id()
                    name()
                    parent(this)
                }
                customFields()
            }
        }
        scope.getAllProfiles(client, batchInfo, buildPartial)
    }
    return profiles.mapNotNull { profile ->
        val profilePictureId = profile.profilePicture ?: return@mapNotNull null
        val russianName = profile.languages.find { it.language.name == "Russian" }?.name
        val location = extractSimpleLocation(profile)
        Colleague(
            profile.id,
            russianName?.firstName ?: profile.name.firstName,
            russianName?.lastName ?: profile.name.lastName,
            profilePictureId,
            profile.memberships.map {
                val ratio = (it.customFields["Ratio"] as? FractionCFValue)?.value
                Membership(it.role.name, it.team.name, it.lead, if (ratio != null) ratio.numerator.toFloat() / ratio.denominator else 1f)
            }.sortedWith(compareBy<Membership>({ it.lead }, { it.ratio }).reversed()),
            location?.presentableName,
            extractLocationTags(profile) + extractTeamTags(profile)
        )
    }
}

fun HttpClientConfig<*>.configureClient() {
    install(HttpTimeout) {
        requestTimeoutMillis = 5000
        connectTimeoutMillis = 5000
        socketTimeoutMillis = 5000
    }
}

fun makeAnkiDeck(colleagues: List<ColleagueWithPicture>, additionalTags: Map<String, Set<String>>) {
    val resDir = File("result")
    val imagesDir = resDir.resolve("images")
    resDir.deleteRecursively()
    imagesDir.mkdirs()

    for (colleague in colleagues) {
        imagesDir.resolve("${colleague.colleague.profilePictureId}.jpg").writeBytes(colleague.picture)
    }
    val rows = colleagues.map { colleague ->
        val allTags = colleague.colleague.tags + (additionalTags[colleague.colleague.id] ?: emptySet())
        colleagueAttributes.map { attribute -> attribute.get(colleague.colleague) }.plus(allTags.joinToString(" "))
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

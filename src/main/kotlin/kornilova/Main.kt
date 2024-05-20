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
import java.util.*

private val preferredNameLang = System.getProperty("preferred.name.lang") // none for english or "russian"

fun main() {
    val scope = Berlin
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

    val resDir = File("result")
    val imagesDir = resDir.resolve("images")
    imagesDir.mkdirs()
    val targetFile = resDir.resolve("result.csv")
    val existingIds = if (targetFile.exists()) csvReader().readAll(targetFile).map { it[0] }.toSet() else emptySet()

    val httpClient = HttpClient {
        configureClient()
    }
    var loadedImagesCount = 0
    var existingImagesCount = 0
    val colleagues = runBlocking {
        fetchColleagues(scope, client, existingIds)
    }.map { colleague ->
        val imageExists = imageFile(imagesDir, colleague).exists()
        val picture = if (imageExists) {
            existingImagesCount += 1
            null
        }
        else {
            loadedImagesCount += 1
            runBlocking {
                loadPicture(token, httpClient, colleague.profilePictureId)
            }
        }
        if ((loadedImagesCount + existingImagesCount) % 10 == 0) {
            println("Fetched $loadedImagesCount photos and $existingImagesCount didn't need to be fetched because they are already saved to disk")
        }
        ColleagueWithPicture(colleague, picture)
    }
    tagsDir.mkdirs()
    makeAnkiDeck(colleagues, additionalTags, targetFile, imagesDir)
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
    existingIds: Set<String>,
    skip: Int = 0
): Sequence<Colleague> {
    var count = 0
    val profiles = fetchAll(scope.initialBatchInfo(skip)) { batchInfo ->
        val buildPartial: TD_MemberProfilePartial.() -> Unit = {
            defaultPartial()
            managers(this)
            locations {
                location {
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
            membershipHistory()
            about()
        }
        val res = scope.getAllProfiles(client, batchInfo, buildPartial)
        count += res.data.size
        println("Fetched $count profiles")
        res
    }
    return profiles.mapNotNull { profile ->
        if (profile.id in existingIds) {
            return@mapNotNull null
        }
        val profilePictureId = profile.profilePicture ?: return@mapNotNull null
        val preferredName = profile.languages.find { it.language.name.lowercase(Locale.ENGLISH) == preferredNameLang }?.name
        val locations = extractLocations(profile)
        val startDate = profile.membershipHistory.mapNotNull { it.since }.min()
        val memberships = profile.memberships.map {
            val ratio = (it.customFields["Ratio"] as? FractionCFValue)?.value
            Membership(
                it.role.name,
                it.team.name,
                it.lead || it.role.name.contains("Lead"),
                if (ratio != null) ratio.numerator.toFloat() / ratio.denominator else 1f
            )
        }
        Colleague(
            profile.id,
            preferredName?.firstName ?: profile.name.firstName,
            preferredName?.lastName ?: profile.name.lastName,
            profile.languages.mapNotNull { it.name?.let { name -> Pair(name.firstName, name.lastName) } }
                .plus(Pair(profile.name.firstName, profile.name.lastName)),
            profilePictureId,
            memberships.sortedWith(compareBy<Membership>({ it.lead }, { it.ratio }).reversed()),
            locations,
            startDate,
            profile.about,
            locations.flatMapTo(mutableSetOf()) { it.tags } + extractTeamTags(profile) + chooseStartTimeBucket(startDate).tag +
                    (if (memberships.any { it.lead }) setOf("is_lead") else emptySet())
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

fun makeAnkiDeck(
    colleagues: Sequence<ColleagueWithPicture>,
    additionalTags: List<String>,
    targetFile: File,
    imagesDir: File
) {
    colleagues.chunked(10).map { colleaguesChunk ->
        rememberTags(colleaguesChunk, additionalTags)
        colleaguesChunk.map { colleague ->
            if (colleague.picture != null) {
                imageFile(imagesDir, colleague.colleague).writeBytes(colleague.picture)
            }
            val allTagsFromDisk = readAllTags()
            val allTags = colleague.colleague.tags + (allTagsFromDisk[colleague.colleague.id] ?: emptySet())
            colleagueAttributes.map { attribute -> attribute.get(colleague.colleague) }.plus(allTags.joinToString(" "))
        }
    }.forEach { rows ->
        csvWriter().writeAll(rows, targetFile, true)
    }
}

private fun imageFile(imagesDir: File, colleague: Colleague) =
    imagesDir.resolve("${colleague.profilePictureId}.jpg")

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

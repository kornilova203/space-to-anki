# Space-to-Anki

## How to use it

1. Install Anki desktop https://apps.ankiweb.net/
1. First, you need to import "Colleague" [Note Type](https://docs.ankiweb.net/getting-started.html#note-types).
   1. To do it, download the .apkg file from [the latest release](https://github.com/kornilova203/space-to-anki/releases) and import it to Anki. This will import one note with me, and you'll get `Colleague` note type.
1. Obtain token
   1. Go to https://jetbrains.team/
   1. Extensions > API Playground
   1. Click on any example and copy your personal token from request (string after "Authorization: Bearer ")
   1. Paste token to `src/main/resources/token.txt`
1. Open [Main.kt](src/main/kotlin/kornilova/Main.kt) and run the script. It'll start fetching all profiles because of scope `AllScope`. You don't need to wait until it finished, results are continuously saved to disk.
1. Copy all photos from `result/images` directly to [collection.media directly](https://docs.ankiweb.net/files.html#file-locations) of Anki app.
1. Import `result/result.csv` to Anki
   1. File > Import
   1. Choose `result/result.csv`
   1. Field separator = comma
   1. Allow HTML in fields = true
   1. Notetype = Colleague
   1. Deck (I think you'll have deck called `0. Colleagues` after importing .apkg file but you can also choose/create another one)
   1. Under `Field mapping` point `Tags` to last column in CSV
   1. Click import

Anki uses the first field as a unique identifier of a note.
The first field of `Colleage` note type is `Id` from Space.
So you can reimport the same people several times, you won't get duplicates, instead existing notes will be updated.

## Scopes and tags
You don't have to fetch all users, you can specify `Scope` e.g. `EmailsScope`.
You can also set additional tags for users that will be fetched.
Those tags will be persisted to `tags` directory, and they'll be reapplied when you run the script next time even with different scope and tags, so you won't lose them during re-import. 

Usage example:
```kotlin
    val scope = EmailsScope(listOf("emails", "of", "ij-camp", "participants"))
    val additionalTags = listOf<String>("ij-camp")
```

You can create a filtered deck that'll contain only colleagues with specified tag(s).

## Name language

By default `First name` and `Last name` fields will contain name in English.
You can switch preferred language of name using `preferred.name.lang` e.g. `-Dpreferred.name.lang=russian`.

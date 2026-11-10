# Lexis

### Capture words quickly. Understand them in context. Remember them over time.

**Lexis** is an Android vocabulary app concept that brings together a personal word library, quick capture, flashcards, and home screen widgets. Words you encounter in everyday life become part of your own learning system: saved, enriched with meanings and pronunciation, organized into decks, and brought back for review.

The repository is named `quick-vocab`; the product is called **Lexis**.

> This README describes the vision and intended scope of the complete product. Lexis is under development; the features below should not be read as a list of released capabilities.

## Why Lexis?

When you encounter an unfamiliar word in an article, a video, or at work, the first priority is to save it. A word left in a note is easy to forget, while entering every definition, pronunciation, and example immediately interrupts what you were doing.

Lexis aims to connect capture and learning in one continuous experience:

1. **Capture:** save a word straight into the right deck.
2. **Understand:** add meanings, readings, and examples, automatically or manually.
3. **Organize:** search, categorize, and manage your personal vocabulary library.
4. **Remember:** practice with flashcards and spaced repetition.
5. **Revisit:** put words on your home screen and learn during brief moments throughout the day.

The goal is to turn saved vocabulary into knowledge you can use and retain.

## Who is it for?

- Language learners building a personal vocabulary library from real-world content.
- Learners working with vocabulary collections for IELTS, JLPT, or HSK.
- People studying multiple languages who need appropriate support for different writing systems.
- Anyone who wants to use short breaks to revisit and practice words.

## Product principles

- **Capture first, enrich later:** dictionary lookups and connection failures should never block saving a word.
- **Keep core learning local:** your vocabulary library and essential study activities should work on your device.
- **Give users control:** support importing, editing, exporting, and backing up their content.
- **Teach through context:** connect meanings with readings, examples, and translations where available.
- **Keep common actions simple:** adding words, listening to pronunciation, and starting a review should take little effort.
- **Share progress between app and widgets:** review results should feed the same learning history.
- **Support multiple languages from the foundation:** readings and presentation should extend beyond English.

## Planned capabilities

### 1. Personal vocabulary library

Each entry can contain:

- The original word or phrase and its language.
- One or more meanings, translations, and parts of speech.
- Phonetic transcription or readings appropriate to the language.
- Audio pronunciation through system text-to-speech where supported.
- Example sentences, translated examples, and related usage.
- Personal notes, tags, and favorite status.
- Review history and learning progress.

An English entry might show IPA; a Japanese entry might show kanji and kana; a Chinese entry might show hanzi and pinyin. The presentation should preserve the relationship between each meaning, reading, and example, especially for words with multiple senses.

### 2. Quick Add

Quick Add is the primary way to capture a new word:

1. Enter a word or phrase.
2. Detect or select its language.
3. Choose a destination deck.
4. Save immediately and fill in missing details later.

The flow should support capturing several words in succession, detecting existing entries, and opening a newly saved word for editing. If language detection is uncertain, the user can correct it before confirming.

Words should still be saved locally when offline. A word with a manually entered meaning can become available for study without waiting for automatic enrichment.

### 3. Automatic enrichment

Lexis aims to look up and add meanings, phonetic transcriptions, readings, and examples from suitable data sources.

- Use offline dictionaries when the relevant data is available.
- Clearly show pending, processing, ready, and failed states.
- Allow retries when connectivity or the lookup source becomes available again.
- Preserve user edits rather than silently overwriting them.
- Allow manual completion when no matching entry is found.

Dictionary sources, service providers, and language coverage will be selected during implementation. Automatically supplied content should remain open to review and correction.

### 4. Deck management

Users can organize words by goal, topic, or source, for example:

- IELTS 3000.
- Japanese N5.
- Chinese HSK.
- Words from books.
- Workplace vocabulary.

Each deck has a name, description, learning language, and translation language. Planned actions include creating, editing, and deleting decks; adding and removing words; selecting multiple entries; filtering by status; and sorting lists.

A word can belong to multiple decks. Learning progress is shared for the same vocabulary entry so that organizing content does not create duplicate review schedules.

### 5. Search and word details

- Search across the entire library or within a deck.
- Filter by language, deck, learning status, and favorites.
- Search by language-specific readings where the available data supports it.
- View meanings, pronunciation, examples, and progress on one detail page.
- Move quickly between words while browsing a deck.

The goal is to help users find saved content even when they remember only its reading or part of its details.

### 6. Flashcards and spaced repetition

Flashcards encourage active recall before revealing the answer:

1. Show the front of the card.
2. Recall the answer, then reveal it.
3. Rate how well you remembered.
4. Calculate the next review time.

| Rating | Meaning |
| --- | --- |
| Again | Could not recall the answer or answered incorrectly |
| Hard | Recalled with difficulty |
| Good | Recalled with ordinary effort |
| Easy | Recalled easily |

The intended scheduling approach uses **FSRS-6** alongside learning and relearning steps. Review timing adapts to ratings, while a daily new-word limit helps keep the workload manageable.

The review experience should support:

- Reviewing the whole library or a specific deck.
- Prioritizing due cards and short-term learning repeats.
- Previewing the next interval for each rating.
- Undoing an accidental rating.
- Resuming study after interruptions using saved progress.
- Session summaries covering review counts, recall ratings, time spent, and difficult words.
- Additional practice for words that need attention.

Simply viewing a word or marking it as a favorite should not automatically count as a memory assessment.

### 7. Home screen widgets

Widgets are central to the Lexis concept: bring vocabulary to a place users see often and make it possible to learn without repeatedly opening the app.

Each widget can have its own configuration:

- Widget name and source deck.
- Word selection: random, new, learning, or due-first.
- Size and presentation style.
- Visibility of readings, meanings, and examples.
- Whether to include words already learned.
- Manual navigation or periodic updates within Android platform limits.

Planned widget styles:

| Style | Experience |
| --- | --- |
| Classic | Word, reading, meaning, and quick actions |
| Flip card | Show the word first; tap to reveal the answer and rate recall |
| Quick check | A short question to test knowledge |
| Glyph tile | Focus on kanji or hanzi with its reading |
| Clear | A compact layout over the home screen wallpaper |
| Dictionary | A layout inspired by a dictionary entry |
| Daily ring | Daily goals and learning progress |

When a widget supports ratings, its results should update the same review schedule as the app. Widgets can also open a word's detail page for further reading or editing.

### 8. Import and export

Users should be able to start with existing content instead of entering every word manually.

Initial import priorities:

- **CSV:** map columns to words, meanings, readings, and examples.
- **TXT:** one word or phrase per line, with missing information enriched later.
- Import into an existing deck or create a new one.
- Preview entries, validate rows, and detect duplicates.
- Show progress and a summary of import results.

Per-deck CSV export gives users a readable copy of their content for use in other tools. Full backup and restore should also preserve learning progress and relevant settings.

Excel files, Anki `.apkg` decks, and Google Sheets connections are possible extensions after the core import and export flows.

### 9. Progress and study habits

The progress view should clearly answer:

- How many entries are in the library?
- How many words were added, introduced, and reviewed today?
- Which words are being learned, and which need another review?
- How has study activity changed over recent days?
- What is the progress of each deck?

Users can set a daily new-word goal and revisit difficult words after a session. Statistics should distinguish unique words from review events because one word may be rated several times.

### 10. Personalization and data

- Translation language and default deck.
- Daily new-word goal.
- Voice selection, automatic pronunciation, and guidance when a required voice is missing.
- Light, dark, and system appearance.
- Text sizing and readability across writing systems.
- Automatic enrichment controls and an offline-source-only option.
- Management of widgets, stored data, and backups.

Core learning is intended to work locally without requiring sign-in. Accounts and multi-device synchronization are potential extensions that need separate specifications for data handling, conflicts, and privacy.

## A day with Lexis

In the morning, you encounter **meticulous** in an English article. You save it to your work vocabulary deck and continue reading. Lexis adds its reading, meaning, and an example when a suitable data source is available.

During the day, a widget brings the word back with an example sentence. In the evening, you start a review session, recall the meaning before flipping the card, and select **Good**. The next review is scheduled using that result.

The same process can apply to **食べる** in a Japanese deck or **学习** in a Chinese deck, using the appropriate readings and typography.

## Development milestones

| Milestone | Intended outcome |
| --- | --- |
| Data foundation | Multilingual vocabulary, decks, review history, and FSRS scheduling |
| Complete study flow | Create a deck → save a word with a meaning → review flashcards → persist progress |
| Convenient capture | Quick Add, editing, and background enrichment |
| Manageable library | Search, filters, favorites, and CSV/TXT import and export |
| Home screen learning | Configurable deck-based widgets with shared progress |
| Refined experience | Statistics, settings, backups, and accessibility |
| Extensions | More dictionary sources, import formats, and optional synchronization |

These milestones describe the development direction, not promised release dates.

## Technical foundation

The initial platform is native Android:

- **Kotlin**, **Jetpack Compose**, and **Material 3** for the app.
- **Room** for local storage.
- **ViewModel**, **Coroutines**, and **Flow** for state and asynchronous work.
- **FSRS-6** with learning steps for review scheduling.
- Separation of learning logic, data access, and UI so that the app and widgets can share the same behavior.

Application code lives in [app/](app/); reference documents and prototypes live in [design/](design/). Build settings and dependencies are declared in [app/build.gradle.kts](app/build.gradle.kts) and [gradle/libs.versions.toml](gradle/libs.versions.toml).

## License

This repository uses the **GNU Lesser General Public License v3.0 (LGPL-3.0)**. See [LICENSE](LICENSE).

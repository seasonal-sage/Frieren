# Time Capsule — a secure multimedia personal digital journal

A JavaFX grimoire-diary: black cover, candlelight desk, and Frieren, a fairy
guardian. Entries are AES-256 encrypted per keeper, sealable into the future
as time capsules, with photos, voice clips, video clips, habits, stats,
tag search, and JSON/PDF copies.

Team: Adithya G K, Anusri Anilkumar, Rithunanth A V, Ullekh S Prakash ·
Guide: Mrs. Ashila K A · PBCST304 OOP (S3 AI&ML)

## Run it

Needs JDK 17 + Maven. From the project root:

```powershell
mvn javafx:run
```

Or run `com.timecapsule.ui.BookApp` in IntelliJ (Project SDK 17) with VM options:

```
--module-path "<javafx-win-jars>" --add-modules javafx.controls,javafx.fxml,javafx.media
```

Tests: `mvn test` (75 green).

## First run

The cover opens in claim mode — pick a keeper name + secret word. The diary
database lives at `~/.timecapsule/` (safe from rebuilds). Media attachments are
stored in `~/.timecapsule/media/`.

## Features

- **Entries** — write, tag, mood-stamp, and seal entries into the future
- **Time capsules** — sealed entries unlock automatically on their date; Frieren announces when a seal cracks
- **Media** — attach photos, voice clips, and video clips; tap any keepsake to open it in its own window with playback controls
- **Search** — live filter as you type; `#tag` finds tagged entries; `find #tag` in Frieren's chat searches locally
- **Frieren chat** — summon the ✦ tab to talk; she remembers your conversation across restarts (per keeper)
- **Calendar** — ink-dot month view; tap a day to read its entries
- **Habits** — streak tracking with weekly targets
- **Stats** — mood tallies and entry charts
- **Export** — JSON or PDF copies of your entries (sealed pages stay sealed)
- **Back cover** — librarian's notes (recent audit events)

## Data & privacy

- Your AES key is derived from your secret word and held in memory only — never written to disk
- Sealed entry bodies are never decrypted before their unlock date
- All data is keeper-scoped; one keeper cannot read another's entries
- Click "forget" in Frieren's chat to wipe your conversation history

# Time Capsule — a secure multimedia personal digital journal

A JavaFX grimoire-diary: black cover, candlelight desk, and Frieren, a fairy
guardian. Entries are AES-256 encrypted per keeper, sealable into the future
as time capsules, with photos, voice clips, habits, stats, and JSON/PDF copies.

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

Tests: `mvn test` (50 green and counting).

## First run

The cover opens in claim mode — pick a keeper name + secret word. The diary
database lives at `~/.timecapsule/` (safe from rebuilds). Click the ✦ tab to
summon Frieren.

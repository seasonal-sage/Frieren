# Time Capsule — A Magical Notebook for Your Self-Improvement Journey

A JavaFX grimoire-style self-improvement platform: mark your journey, set
milestones, complete them, and seal promises to your future self. Built for
anyone trying to organize their life and overcome procrastination — no
subscriptions, no cloud, everything stays on your machine.

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

Tests: `mvn test` (90 green).

## First run

The cover opens in claim mode — pick a keeper name + secret word. Your diary
lives at `~/.timecapsule/` (safe from rebuilds). Click the ✦ tab to summon
Frieren, your journaling companion.

## Features — your journey, one milestone at a time

**Checkpoints & Goals** — Set milestones for yourself with optional target
dates. Mark them complete when you reach them. Frieren celebrates each win.
See your progress at a glance in the Checkpoints shelf.

**Time Capsules** — Write entries and seal them into the future. They unlock
on their own date — a promise to your future self that you can't read early.
Frieren announces when a seal cracks.

**Entries & Media** — Write daily entries with mood stamps and tags. Attach
photos, voice clips, and video clips. Tap any keepsake to open it in its own
window with playback controls.

**Calendar** — Ink-dot month view showing when you've written. Set daily,
weekly, or monthly reminders that speak to you only when the app opens —
they never nag mid-read.

**Habits** — Streak tracking with weekly targets. Build consistency one day
at a time.

**Stats** — Mood tallies, sentiment trends, entry charts, and checkpoint
completion rates. See your momentum build over time.

**Export** — Download your journey as a PDF or JSON "progress scroll" to
share with a therapist, coach, or keep for yourself.

**Frieren Chat** — Summon the ✦ tab to talk. She remembers your conversation
across restarts and helps you find past entries ("find birthday", "find #tag").
AI is optional — she works offline with scripted lines if Ollama isn't
running.

## Why this helps procrastinators

- **Delayed gratification** — Sealed entries build anticipation; you can't
  cheat and read them early.
- **Visible progress** — Checkpoints, streaks, and stats turn abstract goals
  into something you can see growing.
- **Low friction** — One keeper name, one secret word. No accounts, no
  email, no tracking. Everything is local.
- **Shareable proof** — PDF export lets you show your journey to someone who
  can help, or look back months later and see how far you've come.

## Data & privacy

- Your AES key is derived from your secret word and held in memory only —
  never written to disk.
- Sealed entry bodies are never decrypted before their unlock date.
- All data is keeper-scoped; one keeper cannot read another's entries.
- Your database lives at `~/.timecapsule/timecapsule.db`; media at
  `~/.timecapsule/media/`. Back these up if you start keeping real entries.

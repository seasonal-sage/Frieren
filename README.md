# Time Capsule — A Magical Notebook for Your Self-Improvement Journey

A JavaFX grimoire-style self-improvement platform: mark your journey, set
milestones, complete them, and seal promises to your future self. Built for
anyone trying to organize their life and overcome procrastination — no
subscriptions, no cloud, everything stays on your machine.

**Team:** Adithya G K, Anusri Anilkumar, Rithunanth A V, Ullekh S Prakash  
**Guide:** Mrs. Ashila K A · **Course:** PBCST304 OOP (S3 AI&ML)  
**Tests:** `mvn test` → **90/90 green** (BUILD SUCCESS)  
**Repository:** https://github.com/seasonal-sage/Frieren.git

---

## Screenshots

### Cover & Login
![Cover / claim screen](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/13-cover-claim.png)

### Entries List & Search
![Entries list with seal dots](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/14-entries-list.png)

### Entry Detail View
![Entry detail with mood stamp and media](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/15-entry-detail.png)

### Tag Search
![#tag search and live filter](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/16-search-results.png)

### Calendar & Reminders
![Ink-dot calendar month view](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/17-calendar-month.png)

### Set Reminder
![Inline reminder form](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/18-set-reminder.png)

### Reminders List
![Upcoming reminders list](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/19-reminders-list.png)

### Checkpoints (Goals)
![Pending and completed checkpoints](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/20-checkpoints-pending.png)

![Completed milestones](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/21-checkpoints-completed.png)

### Stats & Habits
![Stats dashboard](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/22-stats-dashboard.png)

![Habits tracker](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/23-habits-tracker.png)

### Media Viewer
![Photo viewer popup](https://raw.githubusercontent.com/seasonal-sage/Frieren/main/screenshots/24-media-photo.png)

---

## Run It

Needs JDK 17 + Maven. From the project root:

```powershell
mvn javafx:run
```

Or run `com.timecapsule.ui.BookApp` in IntelliJ (Project SDK 17) with VM options:

```
--module-path "<javafx-win-jars>" --add-modules javafx.controls,javafx.fxml,javafx.media
```

Run the full test suite:

```powershell
mvn test
```

---

## First Run

The cover opens in **claim mode** — pick a keeper name + secret word. Your diary
lives at `~/.timecapsule/` (safe from rebuilds). Media attachments are stored in
`~/.timecapsule/media/`. Click the ✦ tab to summon Frieren, your journaling
companion.

---

## Features — Your Journey, One Milestone at a Time

### Checkpoints & Goals
Set milestones for yourself with optional target dates. Mark them complete when
you reach them — Frieren celebrates each win. See your progress at a glance in
the Checkpoints shelf.

### Time Capsules
Write entries and seal them into the future. They unlock on their own date — a
promise to your future self that you can't read early. Frieren announces when
a seal cracks.

### Entries & Media
Write daily entries with mood stamps and tags. Attach photos, voice clips, and
video clips. Tap any keepsake to open it in its own window with playback
controls.

### Search & Tags
Live filter as you type. Use `#tag` syntax (e.g., `#birthday`) to search tags
only. Frieren's chat inherits the same search (`find birthday`, `find #tag`).
Sealed entry bodies never leak — titles and tags are searchable even while
locked.

### Calendar — Ink-Dot Month View
See when you've written at a glance. Set daily, weekly, or monthly reminders
that speak to you **only when the app opens** — they never nag mid-read.

### Habits
Streak tracking with weekly targets. Build consistency one day at a time.

### Stats Dashboard
Mood tallies, sentiment trends, entry charts, and checkpoint completion rates.
See your momentum build over time.

### Export — Progress Scroll
Download your journey as a **PDF or JSON** "progress scroll" to share with a
therapist, coach, or keep for yourself. Sealed entries travel as promises —
never exported early.

### Frieren Chat
Summon the ✦ tab to talk. She remembers your conversation across restarts and
helps you find past entries ("find birthday", "find #tag"). AI is optional —
she works offline with scripted lines if Ollama (`qwen2.5:3b`) isn't running.

### Sound Effects
UI clicks, page turns, seal cracks, and entry-saved chimes. Toggle sound on/off
from the Contents spread.

---

## Why This Helps Procrastinators

- **Delayed gratification** — Sealed entries build anticipation; you can't cheat
  and read them early.
- **Visible progress** — Checkpoints, streaks, and stats turn abstract goals
  into something you can see growing.
- **Low friction** — One keeper name, one secret word. No accounts, no email,
  no tracking. Everything is local.
- **Shareable proof** — PDF export lets you show your journey to someone who
  can help, or look back months later and see how far you've come.

---

## Data & Privacy

- Your AES key is derived from your secret word and held in memory only — never
  written to disk.
- Sealed entry bodies are never decrypted before their unlock date.
- All data is keeper-scoped; one keeper cannot read another's entries.
- Your database lives at `~/.timecapsule/timecapsule.db`; media at
  `~/.timecapsule/media/`. Back these up if you start keeping real entries.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **GUI** | JavaFX 17.0.6 (controls, fxml, media) |
| **Database** | SQLite 3.45.3 (local, file-based) |
| **Security** | BCrypt-12 password hashing, AES-256-GCM + PBKDF2 encryption |
| **Export** | Jackson (JSON), OpenPDF (PDF) |
| **Testing** | JUnit 5 (90 test methods, all green) |
| **AI (optional)** | Ollama `qwen2.5:3b` via local HTTP |
| **Build** | Apache Maven |

---

## Recent Bug Fixes

- **Tag dropdown now populates** — shows tag list with counts, fallback prompt when empty
- **Photo viewer window size** — opens at 1200×850 instead of wrapping tightly
- **Audio playback** — `player.play()` moved inside `setOnReady()` guard so WAV clips actually play
- **Video playback** — same `setOnReady()` fix for MP4; duplicate `MediaPlayer` declaration removed
- **Audio track warning** — if a video has no audio track, a friendly message is shown

---

## Roadmap (Future Enhancements)

| Enhancement | Description |
|-------------|-------------|
| **Tier-2 Recall** | Semantic memory via embeddings — Frieren recalls relevant past conversations |
| **Auto-lock** | Session timeout after N minutes of inactivity |
| **Export encryption** | Password-protected PDF backups |
| **jPackage `.exe`** | One-click Windows installer (~200 MB) |
| **Mobile app** | Android/iOS companion |

---

## References

1. Herbert Schildt, *Java: The Complete Reference*, 12th Ed., McGraw-Hill
2. Cay S. Horstmann, *Core Java Volume I — Fundamentals*, Pearson
3. Oracle Java SE Official Documentation
4. JavaFX 17 Official Documentation
5. SQLite Documentation
6. BCrypt & AES-GCM Cryptographic Standards

---

**Thank you — Questions?**

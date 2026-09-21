package com.timecapsule.ui;

import com.timecapsule.core.JournalException;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.core.Mood;
import com.timecapsule.media.Media;
import com.timecapsule.media.MediaException;
import com.timecapsule.media.MediaKind;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyGuide;
import com.timecapsule.ui.widgets.MoodStamp;
import com.timecapsule.ui.widgets.TapedPhoto;
import com.timecapsule.ui.widgets.WaxSeal;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The working heart of the diary (Module: UI, Day 6): the entries list and
 * the entry spread (read + compose + seals + keepsakes). Talks only to
 * {@link JournalManager} — never to DAOs or keys.
 */
public class EntriesView {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final BookNavigator navigator;
    private final FairyGuide frieren;
    private final FairyDialogueProvider lines;
    private final Stage stage;
    private final Supplier<StackPane> contentsScreen;

    private StackPane listSpread;
    private final Map<Long, javafx.scene.media.MediaPlayer> players = new HashMap<>();

    public EntriesView(BookNavigator navigator, FairyGuide frieren, FairyDialogueProvider lines,
                       Stage stage, Supplier<StackPane> contentsScreen) {
        this.navigator = navigator;
        this.frieren = frieren;
        this.lines = lines;
        this.stage = stage;
        this.contentsScreen = contentsScreen;
    }

    /** Opens the entries list with a turn. */
    public void showList() {
        stopPlayers();
        listSpread = buildListSpread();
        navigator.show(listSpread, BookNavigator.Direction.FORWARD);
        frieren.say(lines.lineFor("entries"));
    }

    /** Rebuilds the list silently when the scheduler breaks seals under us. */
    public void refreshIfShowing() {
        if (listSpread != null && navigator.getCurrent() == listSpread) {
            stopPlayers();
            listSpread = buildListSpread();
            navigator.show(listSpread);
        }
    }

    /** Opens one entry (id 0 = blank page for a new memory). */
    public void showDetail(long id) {
        stopPlayers();
        Memory existing = null;
        if (id != 0) {
            try {
                existing = JournalManager.getEntry(id);
            } catch (JournalException e) {
                frieren.say("That page seems to have fallen out. Back to the list.");
                showList();
                return;
            }
        }
        navigator.show(buildDetailSpread(existing), BookNavigator.Direction.FORWARD);
    }

    // ------------------------------------------------------------------ list

    private StackPane buildListSpread() {
        VBox rows = new VBox(6);
        rows.setAlignment(Pos.TOP_LEFT);

        List<Memory> entries = List.of();
        String loadError = null;
        try {
            entries = JournalManager.listEntries();
        } catch (JournalException e) {
            loadError = e.getMessage();
        }

        for (Memory memory : entries) {
            rows.getChildren().add(rowFor(memory));
        }
        if (entries.isEmpty() && loadError == null) {
            Label empty = new Label("No pages yet — press your first memory below.");
            empty.getStyleClass().add("body-soft");
            empty.setWrapText(true);
            rows.getChildren().add(empty);
        }
        if (loadError != null) {
            Label error = new Label(loadError);
            error.getStyleClass().add("field-error");
            error.setWrapText(true);
            rows.getChildren().add(error);
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(430);
        scroll.getStyleClass().add("clear-scroll");

        Label heading = new Label("Entries");
        heading.getStyleClass().add("page-heading");

        Button fresh = new Button("＋  Press a new memory");
        fresh.getStyleClass().add("nav-btn");
        fresh.setOnAction(e -> showDetail(0));

        Button back = new Button("←  Contents");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> navigator.show(contentsScreen.get(), BookNavigator.Direction.BACKWARD));

        VBox left = new VBox(8, heading, scroll, fresh, back);
        left.setAlignment(Pos.TOP_LEFT);
        DiaryPage leftPage = new DiaryPage(left).ruled();

        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER);
        Label count = new Label(entries.size() == 1 ? "One page kept." : entries.size() + " pages kept.");
        count.getStyleClass().add("hand-note");
        Label tip = new Label("Sealed entries open on their own — the red dots below keep their secrets.");
        tip.getStyleClass().add("body-soft");
        tip.setWrapText(true);
        right.getChildren().addAll(count, tip);
        DiaryPage rightPage = new DiaryPage(right);
        rightPage.setAlignment(Pos.CENTER);

        return wrap(DiaryPage.spread(leftPage, rightPage));
    }

    private Node rowFor(Memory memory) {
        Instant now = Instant.now();
        boolean sealed = memory.isLocked() && !memory.isUnlockedAt(now);
        boolean opened = memory.isLocked() && memory.isUnlockedAt(now);

        Region dot = new Region();
        dot.getStyleClass().addAll("seal-dot", sealed ? "sealed" : opened ? "open" : "plain");

        Label title = new Label(memory.getTitle());
        title.getStyleClass().add("entry-title");
        String meta = formatDate(memory.getCreatedAt()) + "  ·  " + memory.getMood().getLabel()
                + (sealed ? "  ·  sealed" : "");
        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("entry-meta");
        VBox texts = new VBox(0, title, metaLabel);

        HBox graphic = new HBox(8, dot, texts);
        graphic.setAlignment(Pos.CENTER_LEFT);

        Button row = new Button();
        row.setGraphic(graphic);
        row.getStyleClass().add("entry-row");
        row.setMaxWidth(Double.MAX_VALUE);
        row.setOnAction(e -> showDetail(memory.getId()));
        return row;
    }

    // ---------------------------------------------------------------- detail

    private StackPane buildDetailSpread(Memory existing) {
        Instant now = Instant.now();
        boolean sealed = existing != null && !existing.isUnlockedAt(now);

        Node leftPage;
        Node rightPage;
        if (sealed) {
            leftPage = sealedLeft(existing, now);
            rightPage = sealedRight();
        } else {
            leftPage = editorLeft(existing);
            rightPage = keepsakesRight(existing);
        }
        return wrap(DiaryPage.spread(leftPage, rightPage));
    }

    private Node sealedLeft(Memory existing, Instant now) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        Label title = new Label(existing.getTitle());
        title.getStyleClass().add("page-heading");
        title.setWrapText(true);
        Button back = backToList();
        box.getChildren().addAll(title, new WaxSeal(existing, now), back);
        DiaryPage page = new DiaryPage(box);
        page.setAlignment(Pos.CENTER);
        return page;
    }

    private Node sealedRight() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        Label lore = new Label("Sealed with red wax.");
        lore.getStyleClass().add("hand-note");
        Label lore2 = new Label("The book itself refuses these lines before their day — even Frieren cannot peek.");
        lore2.getStyleClass().add("body-soft");
        lore2.setWrapText(true);
        box.getChildren().addAll(lore, lore2);
        DiaryPage page = new DiaryPage(box);
        page.setAlignment(Pos.CENTER);
        return page;
    }

    private Node editorLeft(Memory existing) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.TOP_LEFT);

        TextField titleField = new TextField(existing == null ? "" : existing.getTitle());
        titleField.setPromptText("Give this memory a title");
        titleField.getStyleClass().addAll("field", "title-field");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Mood> moodBox = new ComboBox<>();
        moodBox.getItems().addAll(Mood.values());
        moodBox.setValue(existing == null ? null : existing.getMood());
        moodBox.setPromptText("mood");
        moodBox.getStyleClass().add("field");
        MoodStamp stamp = new MoodStamp(existing == null ? null : existing.getMood());
        Label dateLabel = new Label(existing == null ? "today"
                : formatDate(existing.getCreatedAt()));
        dateLabel.getStyleClass().add("body-soft");
        metaRow.getChildren().addAll(moodBox, stamp, dateLabel);
        moodBox.setOnAction(e -> {
            // Restamp live as the keeper picks a mood.
            metaRow.getChildren().set(1, new MoodStamp(moodBox.getValue()));
        });

        TextArea bodyArea = new TextArea(existing == null ? "" : existing.getBody());
        bodyArea.setPromptText("Write…");
        bodyArea.setPrefRowCount(11);
        bodyArea.getStyleClass().add("clear-area");

        TextField tagsField = new TextField(existing == null ? ""
                : String.join(", ", existing.getTags()));
        tagsField.setPromptText("tags, comma, separated");
        tagsField.getStyleClass().add("field");

        DatePicker sealPicker = new DatePicker(existing == null || existing.getUnlockDate() == null
                ? null
                : existing.getUnlockDate().atZone(ZoneId.systemDefault()).toLocalDate());
        sealPicker.setPromptText("pick the day");

        // The seal hides behind a spell: tap to bind, tap again to release.
        Button spellBtn = new Button();
        spellBtn.getStyleClass().add("spell-btn");
        spellBtn.setMaxWidth(Double.MAX_VALUE);
        HBox sealBox = new HBox(10, sealPicker);
        sealBox.setAlignment(Pos.CENTER_LEFT);
        sealBox.setPadding(new Insets(0, 0, 0, 4));
        boolean[] bound = {sealPicker.getValue() != null};
        Runnable refreshSpell = () -> {
            sealBox.setVisible(bound[0]);
            sealBox.setManaged(bound[0]);
            if (bound[0]) {
                if (sealPicker.getValue() == null) {
                    sealPicker.setValue(LocalDate.now().plusDays(1));
                }
                spellBtn.setText("✦ Bound in time — tap to release");
            } else {
                spellBtn.setText("Lock this message under the spell of time");
            }
        };
        spellBtn.setOnAction(e -> {
            bound[0] = !bound[0];
            refreshSpell.run();
        });
        sealPicker.setOnAction(e -> refreshSpell.run());
        refreshSpell.run();

        Label error = new Label();
        error.getStyleClass().add("field-error");
        error.setWrapText(true);

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_LEFT);
        Button save = new Button(existing == null ? "Press into the book" : "Save");
        save.getStyleClass().add("ink-btn");
        save.setOnAction(e -> {
            try {
                Instant seal = bound[0] ? toInstant(sealPicker.getValue()) : null;
                Memory saved = existing == null
                        ? JournalManager.createEntry(titleField.getText(), bodyArea.getText(),
                                moodBox.getValue(), splitTags(tagsField.getText()),
                                seal)
                        : JournalManager.updateEntry(existing.getId(), titleField.getText(),
                                bodyArea.getText(), moodBox.getValue(),
                                splitTags(tagsField.getText()), seal);
                showDetail(saved.getId());
            } catch (JournalException ex) {
                error.setText(ex.getMessage());
            }
        });
        buttons.getChildren().add(save);
        if (existing != null) {
            Button delete = new Button("Tear out");
            delete.getStyleClass().add("ink-btn");
            boolean[] armed = {false};
            delete.setOnAction(e -> {
                if (!armed[0]) {
                    armed[0] = true;
                    delete.setText("Tear it out? Click again");
                    delete.getStyleClass().add("danger-armed");
                    return;
                }
                try {
                    JournalManager.deleteEntry(existing.getId());
                    showList();
                } catch (JournalException ex) {
                    error.setText(ex.getMessage());
                }
            });
            buttons.getChildren().add(delete);
        }
        buttons.getChildren().add(backToList());

        box.getChildren().addAll(titleField, metaRow, bodyArea, tagsField, spellBtn, sealBox, error, buttons);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("clear-scroll");
        return new DiaryPage(scroll).ruled();
    }

    private Node keepsakesRight(Memory existing) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_LEFT);
        Label heading = new Label("Pressed between the pages");
        heading.getStyleClass().add("sub-heading");
        box.getChildren().add(heading);

        if (existing == null) {
            Label hint = new Label("Press this page into the book first — keepsakes follow words.");
            hint.getStyleClass().add("body-soft");
            hint.setWrapText(true);
            box.getChildren().add(hint);
        } else {
            List<Media> media = List.of();
            try {
                media = JournalManager.listMedia(existing.getId());
            } catch (JournalException e) {
                Label error = new Label(e.getMessage());
                error.getStyleClass().add("field-error");
                box.getChildren().add(error);
            }
            FlowPane photos = new FlowPane();
            photos.setHgap(12);
            photos.setVgap(12);
            VBox audioRows = new VBox(6);
            int missing = 0;
            for (Media item : media) {
                if (!Files.isRegularFile(item.getFilePath())) {
                    missing++;
                    continue;
                }
                if (item.kind() == MediaKind.PHOTO) {
                    photos.getChildren().add(new TapedPhoto(item.getFilePath()));
                } else {
                    audioRows.getChildren().add(audioRow(item));
                }
            }
            if (!photos.getChildren().isEmpty()) {
                box.getChildren().add(photos);
            }
            if (!audioRows.getChildren().isEmpty()) {
                box.getChildren().add(audioRows);
            }
            if (missing > 0) {
                Label faded = new Label(missing == 1
                        ? "One keepsake faded (its file is gone)."
                        : missing + " keepsakes faded (their files are gone).");
                faded.getStyleClass().add("body-soft");
                box.getChildren().add(faded);
            }

            Button addPhoto = new Button("＋  Press a photo");
            addPhoto.getStyleClass().add("nav-btn");
            addPhoto.setOnAction(e -> attachWithChooser(existing.getId(), true));
            Button addAudio = new Button("＋  Tuck a voice clip");
            addAudio.getStyleClass().add("nav-btn");
            addAudio.setOnAction(e -> attachWithChooser(existing.getId(), false));
            box.getChildren().addAll(addPhoto, addAudio);
        }

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("clear-scroll");
        return new DiaryPage(scroll);
    }

    private Node audioRow(Media audio) {
        Label name = new Label(audio.getFilePath().getFileName().toString());
        name.getStyleClass().add("body-soft");
        Button play = new Button("▶");
        play.getStyleClass().add("audio-btn");
        play.setOnAction(e -> toggleAudio(audio, play));
        HBox row = new HBox(8, play, name);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void toggleAudio(Media audio, Button button) {
        javafx.scene.media.MediaPlayer current = players.get(audio.getId());
        if (current != null) {
            stopPlayer(audio.getId());
            button.setText("▶");
            return;
        }
        stopPlayers();
        try {
            javafx.scene.media.Media clip =
                    new javafx.scene.media.Media(audio.getFilePath().toUri().toString());
            javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(clip);
            player.setOnEndOfMedia(() -> {
                button.setText("▶");
                stopPlayer(audio.getId());
            });
            player.setOnError(() -> {
                button.setText("▶");
                stopPlayer(audio.getId());
                frieren.say("That clip would not sing. Its magic may have faded.");
            });
            players.put(audio.getId(), player);
            button.setText("■");
            player.play();
        } catch (Exception e) {
            frieren.say("That clip would not sing. Its magic may have faded.");
        }
    }

    private void stopPlayer(long mediaId) {
        javafx.scene.media.MediaPlayer player = players.remove(mediaId);
        if (player != null) {
            try {
                player.stop();
                player.dispose();
            } catch (Exception ignored) {
                // Already gone.
            }
        }
    }

    private void stopPlayers() {
        for (Long id : new ArrayList<>(players.keySet())) {
            stopPlayer(id);
        }
    }

    private void attachWithChooser(long entryId, boolean photos) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(photos ? "Press a photo" : "Tuck a voice clip");
        if (photos) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp", "*.bmp"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Audio", "*.mp3", "*.wav", "*.m4a", "*.ogg", "*.flac"));
        }
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            JournalManager.attachMedia(entryId, file.toPath());
            showDetail(entryId);
        } catch (JournalException | MediaException e) {
            frieren.say("That keepsake would not stick: " + e.getMessage());
        }
    }

    private Button backToList() {
        Button back = new Button("←  Entries");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> showList());
        return back;
    }

    private StackPane wrap(Node spread) {
        StackPane wrapper = new StackPane(spread);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private static String formatDate(Instant moment) {
        if (moment == null) {
            return "undated";
        }
        return DATE_FMT.format(moment.atZone(ZoneId.systemDefault()));
    }

    private static List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static Instant toInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}

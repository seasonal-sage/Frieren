package com.timecapsule.ui;

import com.timecapsule.auth.AuthService;
import com.timecapsule.storage.CheckpointDao;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyGuide;
import com.timecapsule.audio.SoundService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * The milestone shelf (Module: Self-Improvement): a keeper's checkpoints and
 * goals — promises to yourself with optional target dates. Mark them complete
 * when you reach them; Frieren celebrates each one.
 */
public class CheckpointsView {

    private final BookNavigator navigator;
    private final FairyGuide frieren;
    private final FairyDialogueProvider lines;
    private final Supplier<StackPane> contentsScreen;
    private final CheckpointDao checkpointDao = new CheckpointDao();

    public CheckpointsView(BookNavigator navigator, FairyGuide frieren, FairyDialogueProvider lines,
                           Supplier<StackPane> contentsScreen) {
        this.navigator = navigator;
        this.frieren = frieren;
        this.lines = lines;
        this.contentsScreen = contentsScreen;
    }

    public void show() {
        PageTracker.set("checkpoints");
        navigator.show(buildSpread(), BookNavigator.Direction.FORWARD);
        frieren.say(lines.lineFor("stats"));
    }

    private StackPane buildSpread() {
        long userId = AuthService.currentUser().getId();

        List<CheckpointDao.CheckpointRow> pending;
        List<CheckpointDao.CheckpointRow> completed;
        try {
            pending = checkpointDao.listPending(userId);
            completed = checkpointDao.listCompleted(userId);
        } catch (Exception e) {
            pending = List.of();
            completed = List.of();
        }

        VBox left = new VBox(10);
        left.setAlignment(Pos.TOP_LEFT);

        Label heading = new Label("Checkpoints");
        heading.getStyleClass().add("page-heading");
        left.getChildren().add(heading);

        // Add new checkpoint form.
        left.getChildren().add(buildAddForm(userId));

        // Pending checkpoints.
        Label pendingLabel = new Label("In progress — " + pending.size());
        pendingLabel.getStyleClass().add("sub-heading");
        left.getChildren().add(pendingLabel);

        if (pending.isEmpty()) {
            Label empty = new Label("No active goals yet. Set your first milestone below.");
            empty.getStyleClass().add("body-soft");
            empty.setWrapText(true);
            left.getChildren().add(empty);
        } else {
            for (CheckpointDao.CheckpointRow cp : pending) {
                left.getChildren().add(buildPendingRow(cp, userId));
            }
        }

        // Completed checkpoints.
        if (!completed.isEmpty()) {
            Label doneLabel = new Label("Completed — " + completed.size());
            doneLabel.getStyleClass().add("sub-heading");
            left.getChildren().add(doneLabel);
            for (CheckpointDao.CheckpointRow cp : completed) {
                left.getChildren().add(buildCompletedRow(cp, userId));
            }
        }

        Button back = new Button("←  Contents");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> {
            PageTracker.set("contents");
            navigator.show(contentsScreen.get(), BookNavigator.Direction.BACKWARD);
        });
        left.getChildren().add(back);
        DiaryPage leftPage = new DiaryPage(left).ruled();

        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER);
        Label quote = new Label("“A goal not written is only a wish.”");
        quote.getStyleClass().addAll("hand-note");
        Label hint = new Label("Set a milestone, work toward it, and seal it when you reach it.");
        hint.getStyleClass().add("body-soft");
        hint.setWrapText(true);
        right.getChildren().addAll(quote, hint);
        DiaryPage rightPage = new DiaryPage(right);

        StackPane wrapper = new StackPane(DiaryPage.spread(leftPage, rightPage));
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    /** Form to create a new checkpoint. */
    private VBox buildAddForm(long userId) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(0, 0, 8, 0));
        Label setLabel = new Label("New checkpoint");
        setLabel.getStyleClass().add("sub-heading");
        box.getChildren().add(setLabel);

        TextField title = new TextField();
        title.setPromptText("What's your goal?");
        title.getStyleClass().add("field");
        box.getChildren().add(title);

        TextArea description = new TextArea();
        description.setPromptText("Why does this matter? (optional)");
        description.setPrefRowCount(2);
        description.getStyleClass().add("field");
        box.getChildren().add(description);

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Target date (optional)");
        datePicker.getStyleClass().add("field");
        box.getChildren().add(datePicker);

        Button save = new Button("Set milestone");
        save.getStyleClass().add("ink-btn");
        save.setOnAction(e -> {
            String t = title.getText();
            if (t == null || t.isBlank()) {
                return;
            }
            try {
                checkpointDao.insert(userId, t, description.getText(),
                        datePicker.getValue());
                title.clear();
                description.clear();
                datePicker.setValue(null);
                show(); // refresh
            } catch (Exception ex) {
                // Silent — checkpoint creation failure must not block.
            }
        });
        box.getChildren().add(save);
        return box;
    }

    /** Row for a pending checkpoint (click checkbox to complete). */
    private VBox buildPendingRow(CheckpointDao.CheckpointRow cp, long userId) {
        VBox row = new VBox(4);
        row.setAlignment(Pos.TOP_LEFT);

        CheckBox check = new CheckBox(cp.title());
        check.getStyleClass().add("entry-title");
        check.setOnAction(e -> {
            if (check.isSelected()) {
                try {
                    checkpointDao.markComplete(cp.id(), userId);
                    SoundService.instance().playEntrySaved();
                    frieren.announce("Milestone reached — \"" + cp.title() + "\" is complete.");
                    show();
                } catch (Exception ignored) {
                    check.setSelected(false);
                }
            }
        });

        Label desc = new Label(cp.description() == null ? "" : cp.description());
        desc.getStyleClass().add("body-soft");
        desc.setWrapText(true);
        if (cp.description() == null || cp.description().isBlank()) {
            desc.setVisible(false);
            desc.setManaged(false);
        }

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label(cp.targetDate() == null ? "No target date"
                : "Target: " + cp.targetDate());
        dateLabel.getStyleClass().add("entry-meta");
        Button edit = new Button("Edit");
        edit.getStyleClass().add("fairy-x");
        Button del = new Button("Delete");
        del.getStyleClass().add("fairy-x");
        edit.setOnAction(e -> editCheckpoint(cp, userId));
        del.setOnAction(e -> {
            try {
                checkpointDao.delete(cp.id(), userId);
                show();
            } catch (Exception ignored) {
            }
        });
        meta.getChildren().addAll(dateLabel, edit, del);

        row.getChildren().addAll(check, desc, meta);
        return row;
    }

    /** Row for a completed checkpoint (click checkbox to reopen). */
    private VBox buildCompletedRow(CheckpointDao.CheckpointRow cp, long userId) {
        VBox row = new VBox(4);
        row.setAlignment(Pos.TOP_LEFT);

        CheckBox check = new CheckBox("✔ " + cp.title());
        check.getStyleClass().add("entry-title");
        check.setSelected(true);
        check.setOnAction(e -> {
            if (!check.isSelected()) {
                try {
                    checkpointDao.markPending(cp.id(), userId);
                    show();
                } catch (Exception ignored) {
                    check.setSelected(true);
                }
            }
        });

        Label done = new Label("Completed " + (cp.completedAt() != null ? cp.completedAt() : "recently"));
        done.getStyleClass().add("entry-meta");

        Button del = new Button("Delete");
        del.getStyleClass().add("fairy-x");
        del.setOnAction(e -> {
            try {
                checkpointDao.delete(cp.id(), userId);
                show();
            } catch (Exception ignored) {
            }
        });

        HBox meta = new HBox(8, done, del);
        meta.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(check, meta);
        return row;
    }

    /** Inline edit form replacing a pending row. */
    private void editCheckpoint(CheckpointDao.CheckpointRow cp, long userId) {
        // Simple inline edit: replace title and description fields on the row.
        // For now, delete and recreate is handled by the Add form; keep this minimal.
    }
}

package com.timecapsule.ui;

import com.timecapsule.auth.AuthService;
import com.timecapsule.core.JournalException;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.storage.ReminderDao;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyGuide;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * The reminder shelf (Module: Calendar): a keeper's full list of upcoming
 * reminders with edit and delete. Reminders surface only when the app
 * opens, but this view lets the keeper curate them at any time.
 */
public class RemindersView {

    private final BookNavigator navigator;
    private final FairyGuide frieren;
    private final FairyDialogueProvider lines;
    private final Supplier<StackPane> contentsScreen;
    private final ReminderDao reminderDao = new ReminderDao();

    public RemindersView(BookNavigator navigator, FairyGuide frieren, FairyDialogueProvider lines,
                         Supplier<StackPane> contentsScreen) {
        this.navigator = navigator;
        this.frieren = frieren;
        this.lines = lines;
        this.contentsScreen = contentsScreen;
    }

    public void show() {
        PageTracker.set("reminders");
        navigator.show(buildSpread(), BookNavigator.Direction.FORWARD);
        frieren.say(lines.lineFor("calendar"));
    }

    private StackPane buildSpread() {
        long userId = AuthService.currentUser().getId();
        List<ReminderDao.ReminderRow> reminders;
        try {
            reminders = reminderDao.listUpcoming(userId);
        } catch (Exception e) {
            reminders = List.of();
        }

        VBox left = new VBox(8);
        left.setAlignment(Pos.TOP_LEFT);

        Label heading = new Label("Reminders");
        heading.getStyleClass().add("page-heading");
        left.getChildren().add(heading);

        if (reminders.isEmpty()) {
            Label empty = new Label("No reminders set yet — the calendar is quiet.");
            empty.getStyleClass().add("body-soft");
            empty.setWrapText(true);
            left.getChildren().add(empty);
        } else {
            for (ReminderDao.ReminderRow r : reminders) {
                VBox row = new VBox(4);
                Label msg = new Label(r.message());
                msg.getStyleClass().add("body-soft");
                msg.setWrapText(true);
                Label meta = new Label(r.reminderDate() + " · " + r.repeatType()
                        + (r.entryId() != null ? " · linked entry" : ""));
                meta.getStyleClass().add("body-soft");
                HBox actions = new HBox(6);
                actions.setAlignment(Pos.CENTER_LEFT);
                Button del = new Button("Delete");
                del.getStyleClass().add("fairy-x");
                del.setOnAction(e -> {
                    try {
                        reminderDao.delete(r.id(), userId);
                        show();
                    } catch (Exception ignored) {
                    }
                });
                actions.getChildren().add(del);
                row.getChildren().addAll(msg, meta, actions);
                left.getChildren().add(row);
            }
        }

        // Set a new reminder form.
        VBox setForm = buildSetReminderForm();
        left.getChildren().add(setForm);

        Button back = new Button("←  Contents");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> {
            PageTracker.set("contents");
            navigator.show(contentsScreen.get(), BookNavigator.Direction.BACKWARD);
        });
        left.getChildren().add(back);
        DiaryPage leftPage = new DiaryPage(left);

        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER);
        Label quote = new Label("“A reminder is a promise to your future self.”");
        quote.getStyleClass().addAll("hand-note");
        Label hint = new Label("Reminders speak only when the grimoire opens — they never nag mid-read.");
        hint.getStyleClass().add("body-soft");
        hint.setWrapText(true);
        right.getChildren().addAll(quote, hint);
        DiaryPage rightPage = new DiaryPage(right);

        StackPane wrapper = new StackPane(DiaryPage.spread(leftPage, rightPage));
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private VBox buildSetReminderForm() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(16, 0, 0, 0));
        Label setLabel = new Label("New reminder");
        setLabel.getStyleClass().add("sub-heading");
        box.getChildren().add(setLabel);

        TextField message = new TextField();
        message.setPromptText("What should I remind you?");
        message.getStyleClass().add("field");
        box.getChildren().add(message);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("field");
        box.getChildren().add(datePicker);

        ComboBox<String> repeatBox = new ComboBox<>();
        repeatBox.getItems().addAll("None", "Daily", "Weekly", "Monthly");
        repeatBox.setValue("None");
        repeatBox.getStyleClass().add("field");
        box.getChildren().add(repeatBox);

        Button save = new Button("Set reminder");
        save.getStyleClass().add("ink-btn");
        save.setOnAction(e -> {
            String msg = message.getText();
            if (msg == null || msg.isBlank()) {
                return;
            }
            try {
                String repeat = repeatBox.getValue();
                String repeatKey = switch (repeat) {
                    case "Daily" -> "daily";
                    case "Weekly" -> "weekly";
                    case "Monthly" -> "monthly";
                    default -> "none";
                };
                reminderDao.insert(AuthService.currentUser().getId(),
                        datePicker.getValue(), msg, null, repeatKey);
                message.clear();
                show();
            } catch (Exception ex) {
                // Silent — reminder creation failure must not block.
            }
        });
        box.getChildren().add(save);
        return box;
    }
}

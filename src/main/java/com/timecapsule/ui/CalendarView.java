package com.timecapsule.ui;

import com.timecapsule.core.JournalException;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.storage.ReminderDao;
import com.timecapsule.auth.AuthService;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyGuide;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The ink-dot calendar (Module: UI, Phase 2): a month grid where written
 * days wear dots; picking a day lists its pages and reminders. Keeper can
 * set one-time or repeating reminders (daily / weekly / monthly) that
 * surface only when the app opens.
 */
public class CalendarView {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final String[] WEEKDAYS = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

    private final BookNavigator navigator;
    private final FairyGuide frieren;
    private final FairyDialogueProvider lines;
    private final Supplier<StackPane> contentsScreen;
    private final EntriesView entries;
    private final ReminderDao reminderDao = new ReminderDao();

    private YearMonth shown = YearMonth.now();
    private LocalDate selected = LocalDate.now();

    public CalendarView(BookNavigator navigator, FairyGuide frieren, FairyDialogueProvider lines,
                        Supplier<StackPane> contentsScreen, EntriesView entries) {
        this.navigator = navigator;
        this.frieren = frieren;
        this.lines = lines;
        this.contentsScreen = contentsScreen;
        this.entries = entries;
    }

    public void show() {
        PageTracker.set("calendar");
        shown = YearMonth.now();
        selected = LocalDate.now();
        navigator.show(buildSpread(), BookNavigator.Direction.FORWARD);
        frieren.say(lines.lineFor("calendar"));
    }

    private void flip(int months) {
        shown = shown.plusMonths(months);
        navigator.show(buildSpread(), months > 0
                ? BookNavigator.Direction.FORWARD : BookNavigator.Direction.BACKWARD);
    }

    private void pick(LocalDate day) {
        selected = day;
        navigator.show(buildSpread());
    }

    private StackPane buildSpread() {
        Map<LocalDate, List<Memory>> byDay;
        try {
            byDay = JournalManager.listEntries().stream()
                    .filter(m -> m.getCreatedAt() != null)
                    .collect(Collectors.groupingBy(
                            m -> m.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()));
        } catch (JournalException e) {
            byDay = Map.of();
        }

        // Reminders that touch this month (for day-cell badges).
        Map<LocalDate, List<ReminderDao.ReminderRow>> remindersByDay;
        try {
            LocalDate monthStart = shown.atDay(1);
            LocalDate monthEnd = shown.atEndOfMonth();
            remindersByDay = reminderDao.listUpcoming(AuthService.currentUser().getId()).stream()
                    .filter(r -> r.reminderDate() != null
                            && !r.reminderDate().isBefore(monthStart)
                            && !r.reminderDate().isAfter(monthEnd))
                    .collect(Collectors.groupingBy(ReminderDao.ReminderRow::reminderDate));
        } catch (Exception e) {
            remindersByDay = Map.of();
        }

        VBox left = new VBox(8);
        left.setAlignment(Pos.TOP_LEFT);

        Button prev = new Button("◀");
        prev.getStyleClass().add("nav-btn");
        prev.setOnAction(e -> flip(-1));
        Button next = new Button("▶");
        next.getStyleClass().add("nav-btn");
        next.setOnAction(e -> flip(1));
        Label month = new Label(MONTH_FMT.format(shown));
        month.getStyleClass().add("sub-heading");
        HBox header = new HBox(12, prev, month, next);
        header.setAlignment(Pos.CENTER);
        left.getChildren().add(header);

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setAlignment(Pos.CENTER);
        for (int c = 0; c < 7; c++) {
            Label day = new Label(WEEKDAYS[c]);
            day.getStyleClass().add("cal-head");
            grid.add(day, c, 0);
        }
        int lead = shown.atDay(1).getDayOfWeek().getValue() - 1;
        int length = shown.lengthOfMonth();
        LocalDate today = LocalDate.now();
        for (int d = 1; d <= length; d++) {
            LocalDate date = shown.atDay(d);
            boolean inked = byDay.containsKey(date);
            List<ReminderDao.ReminderRow> dayReminders = remindersByDay.getOrDefault(date, List.of());
            boolean reminded = !dayReminders.isEmpty();
            Button cell = new Button(String.valueOf(d)
                    + (inked ? " ●" : "")
                    + (reminded ? " ⏰" : ""));
            cell.getStyleClass().add("cal-day");
            if (inked) {
                cell.getStyleClass().add("has-ink");
            }
            if (reminded) {
                cell.getStyleClass().add("has-reminder");
            }
            if (date.equals(today)) {
                cell.getStyleClass().add("today");
            }
            if (date.equals(selected)) {
                cell.getStyleClass().add("selected");
            }
            cell.setOnAction(e -> pick(date));
            grid.add(cell, (lead + d - 1) % 7, (lead + d - 1) / 7 + 1);
        }
        left.getChildren().add(grid);

        Button back = new Button("←  Contents");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> {
            PageTracker.set("contents");
            navigator.show(contentsScreen.get(), BookNavigator.Direction.BACKWARD);
        });
        left.getChildren().add(back);
        DiaryPage leftPage = new DiaryPage(left);

        VBox right = new VBox(8);
        right.setAlignment(Pos.TOP_LEFT);
        Label dayHeading = new Label(DAY_FMT.format(selected));
        dayHeading.getStyleClass().add("sub-heading");
        right.getChildren().add(dayHeading);

        // Entries written this day.
        List<Memory> todays = byDay.getOrDefault(selected, List.of());
        if (!todays.isEmpty()) {
            Label entriesLabel = new Label("Entries");
            entriesLabel.getStyleClass().add("body-soft");
            right.getChildren().add(entriesLabel);
            for (Memory memory : todays) {
                Button title = new Button(memory.getTitle());
                title.getStyleClass().add("nav-btn");
                title.setOnAction(e -> entries.showDetail(memory.getId()));
                right.getChildren().add(title);
            }
        } else {
            Label empty = new Label("Nothing written this day.");
            empty.getStyleClass().add("body-soft");
            right.getChildren().add(empty);
        }

        // Reminders for this day.
        List<ReminderDao.ReminderRow> dayReminders = remindersByDay.getOrDefault(selected, List.of());
        if (!dayReminders.isEmpty()) {
            Label remindersLabel = new Label("Reminders");
            remindersLabel.getStyleClass().add("body-soft");
            right.getChildren().add(remindersLabel);
            for (ReminderDao.ReminderRow r : dayReminders) {
                VBox reminderRow = new VBox(4);
                Label msg = new Label(r.message());
                msg.getStyleClass().add("body-soft");
                msg.setWrapText(true);
                Label meta = new Label(r.repeatType() + (r.entryId() != null ? " · entry" : ""));
                meta.getStyleClass().add("body-soft");
                Button del = new Button("×");
                del.getStyleClass().add("fairy-x");
                del.setOnAction(ev -> {
                    try {
                        reminderDao.delete(r.id(), AuthService.currentUser().getId());
                        navigator.show(buildSpread());
                    } catch (Exception ignored) {
                    }
                });
                HBox metaRow = new HBox(6, meta, del);
                metaRow.setAlignment(Pos.CENTER_LEFT);
                reminderRow.getChildren().addAll(msg, metaRow);
                right.getChildren().add(reminderRow);
            }
        }

        // Set a new reminder for this day.
        VBox setReminder = buildSetReminderForm(selected);
        right.getChildren().add(setReminder);

        DiaryPage rightPage = new DiaryPage(right).ruled();

        StackPane wrapper = new StackPane(DiaryPage.spread(leftPage, rightPage));
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    /** Small form to create a reminder for the given day. */
    private VBox buildSetReminderForm(LocalDate forDate) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(8, 0, 0, 0));
        Label setLabel = new Label("Set a reminder");
        setLabel.getStyleClass().add("sub-heading");
        box.getChildren().add(setLabel);

        TextField message = new TextField();
        message.setPromptText("What should I remind you?");
        message.getStyleClass().add("field");
        box.getChildren().add(message);

        DatePicker datePicker = new DatePicker(forDate);
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
                navigator.show(buildSpread());
            } catch (Exception ex) {
                // Silent — reminder creation failure must not block the calendar.
            }
        });
        box.getChildren().add(save);
        return box;
    }
}

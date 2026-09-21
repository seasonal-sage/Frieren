package com.timecapsule.ui;

import com.timecapsule.core.JournalException;
import com.timecapsule.habits.Habit;
import com.timecapsule.habits.HabitService;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyGuide;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * The practice page (Module: UI, Phase 2): habits with streak stars,
 * weekly aims, today-marks, and a small form for new practices.
 */
public class HabitsView {

    private final BookNavigator navigator;
    private final FairyGuide frieren;
    private final FairyDialogueProvider lines;
    private final Supplier<StackPane> contentsScreen;

    public HabitsView(BookNavigator navigator, FairyGuide frieren, FairyDialogueProvider lines,
                      Supplier<StackPane> contentsScreen) {
        this.navigator = navigator;
        this.frieren = frieren;
        this.lines = lines;
        this.contentsScreen = contentsScreen;
    }

    public void show() {
        navigator.show(buildSpread(), BookNavigator.Direction.FORWARD);
        frieren.say(lines.lineFor("habits"));
    }

    private void refresh() {
        navigator.show(buildSpread());
    }

    private StackPane buildSpread() {
        VBox left = new VBox(8);
        left.setAlignment(Pos.TOP_LEFT);
        Label heading = new Label("Practices");
        heading.getStyleClass().add("page-heading");
        left.getChildren().add(heading);

        List<Habit> habits = List.of();
        String loadError = null;
        try {
            habits = HabitService.listHabits();
        } catch (JournalException e) {
            loadError = e.getMessage();
        }
        if (habits.isEmpty() && loadError == null) {
            Label empty = new Label("No practices yet — begin one below, keeper.");
            empty.getStyleClass().add("body-soft");
            empty.setWrapText(true);
            left.getChildren().add(empty);
        }
        habits.forEach(habit -> left.getChildren().add(cardFor(habit)));
        if (loadError != null) {
            Label error = new Label(loadError);
            error.getStyleClass().add("field-error");
            left.getChildren().add(error);
        }

        Label error = new Label();
        error.getStyleClass().add("field-error");
        TextField nameField = new TextField();
        nameField.setPromptText("a new practice, e.g. Morning pages");
        nameField.getStyleClass().add("field");
        ComboBox<Integer> aimBox = new ComboBox<>();
        for (int i = 1; i <= 7; i++) {
            aimBox.getItems().add(i);
        }
        aimBox.setValue(3);
        aimBox.setPromptText("days / week");
        Button add = new Button("Begin");
        add.getStyleClass().add("ink-btn");
        add.setOnAction(e -> {
            try {
                HabitService.createHabit(nameField.getText(), aimBox.getValue());
                refresh();
            } catch (JournalException ex) {
                error.setText(ex.getMessage());
            }
        });
        HBox form = new HBox(8, nameField, aimBox, add);
        form.setAlignment(Pos.CENTER_LEFT);
        left.getChildren().addAll(form, error);

        Button back = new Button("←  Contents");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> navigator.show(contentsScreen.get(), BookNavigator.Direction.BACKWARD));
        left.getChildren().add(back);
        DiaryPage leftPage = new DiaryPage(left).ruled();

        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER);
        Label count = new Label(habits.size() == 1 ? "One practice kept." : habits.size() + " practices kept.");
        count.getStyleClass().add("hand-note");
        Label lore = new Label("Small marks, kept daily, move mountains. Or so the elves say.");
        lore.getStyleClass().add("body-soft");
        lore.setWrapText(true);
        right.getChildren().addAll(count, lore);
        DiaryPage rightPage = new DiaryPage(right);
        rightPage.setAlignment(Pos.CENTER);

        StackPane wrapper = new StackPane(DiaryPage.spread(leftPage, rightPage));
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private VBox cardFor(Habit habit) {
        int streak;
        int week;
        boolean doneToday;
        try {
            streak = HabitService.streak(habit.getId());
            week = HabitService.weekCount(habit.getId());
            doneToday = HabitService.isDoneOn(habit.getId(), LocalDate.now());
        } catch (JournalException e) {
            Label error = new Label(e.getMessage());
            error.getStyleClass().add("field-error");
            return new VBox(error);
        }

        Label name = new Label(habit.getName());
        name.getStyleClass().add("entry-title");
        Label stars = new Label(streak == 1 ? "★ 1-day streak" : "★ " + streak + "-day streak");
        stars.getStyleClass().add("streak-label");
        HBox top = new HBox(10, name, stars);
        top.setAlignment(Pos.CENTER_LEFT);

        Label progress = new Label(week + " / " + habit.getTargetPerWeek() + " this week");
        progress.getStyleClass().add("entry-meta");

        Button mark = new Button(doneToday ? "Unmark today" : "Mark today");
        mark.getStyleClass().add("ink-btn");
        mark.setOnAction(e -> {
            HabitService.logToday(habit.getId(), !doneToday);
            refresh();
        });
        Button tear = new Button("Let go");
        tear.getStyleClass().add("ink-btn");
        boolean[] armed = {false};
        tear.setOnAction(e -> {
            if (!armed[0]) {
                armed[0] = true;
                tear.setText("Let it go? Click again");
                tear.getStyleClass().add("danger-armed");
                return;
            }
            HabitService.deleteHabit(habit.getId());
            refresh();
        });
        HBox buttons = new HBox(8, mark, tear);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(2, top, progress, buttons);
        card.setAlignment(Pos.TOP_LEFT);
        return card;
    }
}

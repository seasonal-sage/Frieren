package com.timecapsule.ui;

import com.timecapsule.analytics.AnalyticsService;
import com.timecapsule.core.JournalException;
import com.timecapsule.core.Mood;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyGuide;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reading the stars (Module: UI, Phase 2): mood tallies, a sentiment word,
 * and a 14-day pages chart — all derived live from the diary.
 */
public class StatsView {

    private static final DateTimeFormatter AXIS_FMT = DateTimeFormatter.ofPattern("d MMM");

    private final BookNavigator navigator;
    private final FairyGuide frieren;
    private final FairyDialogueProvider lines;
    private final Supplier<StackPane> contentsScreen;

    public StatsView(BookNavigator navigator, FairyGuide frieren, FairyDialogueProvider lines,
                     Supplier<StackPane> contentsScreen) {
        this.navigator = navigator;
        this.frieren = frieren;
        this.lines = lines;
        this.contentsScreen = contentsScreen;
    }

    public void show() {
        navigator.show(buildSpread(), BookNavigator.Direction.FORWARD);
        frieren.say(lines.lineFor("stats"));
    }

    private StackPane buildSpread() {
        VBox left = new VBox(8);
        left.setAlignment(Pos.TOP_LEFT);
        Label heading = new Label("Reading the stars");
        heading.getStyleClass().add("page-heading");
        left.getChildren().add(heading);

        try {
            Map<Mood, Long> counts = AnalyticsService.moodCounts(30);
            for (Mood mood : Mood.values()) {
                long count = counts.getOrDefault(mood, 0L);
                Label name = new Label(mood.getLabel());
                name.getStyleClass().add("entry-title");
                name.setPrefWidth(110);
                Region bar = new Region();
                bar.getStyleClass().add("stat-bar");
                bar.setPrefSize(Math.max(8, count * 36), 14);
                Label number = new Label(String.valueOf(count));
                number.getStyleClass().add("entry-meta");
                HBox row = new HBox(10, name, bar, number);
                row.setAlignment(Pos.CENTER_LEFT);
                left.getChildren().add(row);
            }
            double score = AnalyticsService.sentimentScore(30);
            Label winds = new Label(Double.isNaN(score)
                    ? "No winds yet — write something first."
                    : "The winds feel " + wordFor(score) + " (" + rounded(score) + ").");
            winds.getStyleClass().add("hand-note");
            winds.setWrapText(true);
            left.getChildren().add(winds);
        } catch (JournalException e) {
            Label error = new Label(e.getMessage());
            error.getStyleClass().add("field-error");
            left.getChildren().add(error);
        }

        Button back = new Button("←  Contents");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> navigator.show(contentsScreen.get(), BookNavigator.Direction.BACKWARD));
        left.getChildren().add(back);
        DiaryPage leftPage = new DiaryPage(left).ruled();

        VBox right = new VBox(8);
        right.setAlignment(Pos.TOP_LEFT);
        Label chartHeading = new Label("Pages per day — 14 days");
        chartHeading.getStyleClass().add("sub-heading");
        right.getChildren().add(chartHeading);
        try {
            right.getChildren().add(chart());
        } catch (JournalException e) {
            Label error = new Label(e.getMessage());
            error.getStyleClass().add("field-error");
            right.getChildren().add(error);
        }
        DiaryPage rightPage = new DiaryPage(right);

        StackPane wrapper = new StackPane(DiaryPage.spread(leftPage, rightPage));
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private LineChart<String, Number> chart() {
        CategoryAxis x = new CategoryAxis();
        x.setLabel("");
        NumberAxis y = new NumberAxis();
        y.setLabel("");
        y.setForceZeroInRange(true);
        y.setTickUnit(1);
        y.setMinorTickCount(0);

        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(420);
        chart.getStyleClass().add("star-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("pages");
        AnalyticsService.entriesPerDay(14).forEach((day, count) ->
                series.getData().add(new XYChart.Data<>(AXIS_FMT.format(day), count)));
        chart.getData().add(series);
        return chart;
    }

    private static String wordFor(double score) {
        if (score >= 1.5) {
            return "radiant";
        }
        if (score >= 0.5) {
            return "warm";
        }
        if (score > -0.5) {
            return "calm";
        }
        if (score > -1.5) {
            return "heavy";
        }
        return "stormy";
    }

    private static String rounded(double score) {
        return (score >= 0 ? "+" : "") + Math.round(score * 10) / 10.0;
    }
}

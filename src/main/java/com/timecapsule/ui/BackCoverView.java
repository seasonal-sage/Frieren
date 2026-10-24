package com.timecapsule.ui;

import com.timecapsule.audit.AuditLogger;
import com.timecapsule.audit.AuditLogger.AuditEvent;
import com.timecapsule.export.ExportService;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyGuide;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * The book's back matter (Module: UI, Day 7): "Make a copy" (JSON/PDF
 * export) and the "Librarian's notes" (recent audit events, read-only).
 */
public class BackCoverView {

    private final BookNavigator navigator;
    private final FairyGuide frieren;
    private final FairyDialogueProvider lines;
    private final Supplier<StackPane> contentsScreen;

    public BackCoverView(BookNavigator navigator, FairyGuide frieren, FairyDialogueProvider lines,
                         Supplier<StackPane> contentsScreen) {
        this.navigator = navigator;
        this.frieren = frieren;
        this.lines = lines;
        this.contentsScreen = contentsScreen;
    }

    public void show() {
        PageTracker.set("export");
        navigator.show(buildSpread(), BookNavigator.Direction.FORWARD);
        frieren.say(lines.lineFor("export"));
    }

    private StackPane buildSpread() {
        VBox left = new VBox(10);
        left.setAlignment(Pos.TOP_LEFT);
        Label heading = new Label("Copies");
        heading.getStyleClass().add("page-heading");
        Label desc = new Label("Copies for safekeeping. Sealed pages travel as promises, never early words.");
        desc.getStyleClass().add("body-soft");
        desc.setWrapText(true);

        Label status = new Label();
        status.getStyleClass().add("body-soft");
        status.setWrapText(true);

        Button json = new Button("Copy as JSON");
        json.getStyleClass().add("ink-btn");
        json.setOnAction(e -> export(ExportService.Format.JSON, status));
        Button pdf = new Button("Copy as PDF");
        pdf.getStyleClass().add("ink-btn");
        pdf.setOnAction(e -> export(ExportService.Format.PDF, status));

        Button back = new Button("←  Contents");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> {
            PageTracker.set("contents");
            navigator.show(contentsScreen.get(), BookNavigator.Direction.BACKWARD);
        });

        left.getChildren().addAll(heading, desc, json, pdf, status, back);
        DiaryPage leftPage = new DiaryPage(left).ruled();

        VBox notes = new VBox(6);
        notes.setAlignment(Pos.TOP_LEFT);
        List<AuditEvent> events = AuditLogger.recent(20);
        if (events.isEmpty()) {
            Label empty = new Label("The librarian's page is blank — nothing has happened yet.");
            empty.getStyleClass().add("body-soft");
            empty.setWrapText(true);
            notes.getChildren().add(empty);
        }
        for (AuditEvent event : events) {
            Label row = new Label("· " + event.event() + "   (" + shortDate(event.createdAt()) + ")");
            row.getStyleClass().add("entry-meta");
            row.setWrapText(true);
            notes.getChildren().add(row);
        }
        ScrollPane scroll = new ScrollPane(notes);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(430);
        scroll.getStyleClass().add("clear-scroll");

        Label notesHeading = new Label("Librarian's notes");
        notesHeading.getStyleClass().add("sub-heading");
        VBox right = new VBox(8, notesHeading, scroll);
        right.setAlignment(Pos.TOP_LEFT);
        DiaryPage rightPage = new DiaryPage(right);

        StackPane wrapper = new StackPane(DiaryPage.spread(leftPage, rightPage));
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private void export(ExportService.Format format, Label status) {
        try {
            Path file = ExportService.exportCurrentUser(format, ExportService.defaultDirectory());
            status.getStyleClass().remove("field-error");
            if (!status.getStyleClass().contains("body-soft")) {
                status.getStyleClass().add("body-soft");
            }
            status.setText("Kept at " + file);
        } catch (Exception e) {
            status.getStyleClass().remove("body-soft");
            if (!status.getStyleClass().contains("field-error")) {
                status.getStyleClass().add("field-error");
            }
            status.setText(e.getMessage());
            frieren.say("The copy would not take: " + e.getMessage());
        }
    }

    private static String shortDate(String createdAt) {
        if (createdAt == null) {
            return "undated";
        }
        return createdAt.length() >= 10 ? createdAt.substring(0, 10) : createdAt;
    }
}

package com.timecapsule.ui;

import com.timecapsule.core.JournalException;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyGuide;
import com.timecapsule.ui.widgets.WaxSeal;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The vault of sealed pages (Module: UI, Phase 2): capsules still waiting
 * on the future and ones whose seals have lifted — all openable for reading.
 */
public class CapsulesView {

    private final BookNavigator navigator;
    private final FairyGuide frieren;
    private final FairyDialogueProvider lines;
    private final Supplier<StackPane> contentsScreen;
    private final EntriesView entries;

    public CapsulesView(BookNavigator navigator, FairyGuide frieren, FairyDialogueProvider lines,
                        Supplier<StackPane> contentsScreen, EntriesView entries) {
        this.navigator = navigator;
        this.frieren = frieren;
        this.lines = lines;
        this.contentsScreen = contentsScreen;
        this.entries = entries;
    }

    public void show() {
        navigator.show(buildSpread(), BookNavigator.Direction.FORWARD);
        frieren.say(lines.lineFor("capsules"));
    }

    private StackPane buildSpread() {
        Instant now = Instant.now();
        List<Memory> capsules = List.of();
        String loadError = null;
        try {
            capsules = JournalManager.listEntries().stream()
                    .filter(Memory::isLocked)
                    .collect(Collectors.toList());
        } catch (JournalException e) {
            loadError = e.getMessage();
        }

        List<Memory> sealed = capsules.stream()
                .filter(m -> !m.isUnlockedAt(now))
                .sorted(Comparator.comparing(Memory::getUnlockDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        List<Memory> opened = capsules.stream()
                .filter(m -> m.isUnlockedAt(now))
                .sorted(Comparator.comparing(Memory::getUnlockDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        VBox left = new VBox(8);
        left.setAlignment(Pos.TOP_LEFT);
        Label heading = new Label("Sealed capsules");
        heading.getStyleClass().add("page-heading");
        left.getChildren().add(heading);

        left.getChildren().add(sectionLabel("Still sealed"));
        if (sealed.isEmpty() && loadError == null) {
            left.getChildren().add(hint("None — the wax rests."));
        }
        sealed.forEach(m -> left.getChildren().add(rowFor(m, now)));

        left.getChildren().add(sectionLabel("Opened"));
        if (opened.isEmpty() && loadError == null) {
            left.getChildren().add(hint("None yet — sealed pages open on their own."));
        }
        opened.forEach(m -> left.getChildren().add(rowFor(m, now)));

        if (loadError != null) {
            Label error = new Label(loadError);
            error.getStyleClass().add("field-error");
            left.getChildren().add(error);
        }
        Button back = new Button("←  Contents");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> navigator.show(contentsScreen.get(), BookNavigator.Direction.BACKWARD));
        left.getChildren().add(back);
        DiaryPage leftPage = new DiaryPage(left).ruled();

        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER);
        Label count = new Label(sealed.size() + " waiting · " + opened.size() + " opened");
        count.getStyleClass().add("hand-note");
        Label lore = new Label("Red dots are promises. Gold ones are memories come true.");
        lore.getStyleClass().add("body-soft");
        lore.setWrapText(true);
        right.getChildren().addAll(count, lore);
        DiaryPage rightPage = new DiaryPage(right);
        rightPage.setAlignment(Pos.CENTER);

        StackPane wrapper = new StackPane(DiaryPage.spread(leftPage, rightPage));
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private Node rowFor(Memory memory, Instant now) {
        boolean sealed = !memory.isUnlockedAt(now);
        Region dot = new Region();
        dot.getStyleClass().addAll("seal-dot", sealed ? "sealed" : "open");

        Label title = new Label(memory.getTitle());
        title.getStyleClass().add("entry-title");
        Label meta = new Label(sealed
                ? WaxSeal.countdown(memory.getUnlockDate(), now)
                : "opened — read freely");
        meta.getStyleClass().add("entry-meta");
        VBox texts = new VBox(0, title, meta);

        HBox graphic = new HBox(8, dot, texts);
        graphic.setAlignment(Pos.CENTER_LEFT);

        Button row = new Button();
        row.setGraphic(graphic);
        row.getStyleClass().add("entry-row");
        row.setMaxWidth(Double.MAX_VALUE);
        row.setOnAction(e -> entries.showDetail(memory.getId()));
        return row;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sub-heading");
        return label;
    }

    private Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("body-soft");
        label.setWrapText(true);
        return label;
    }
}

package com.timecapsule.ui.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * One diary page: parchment skin (from diary.css) plus comfortable padding.
 * Call {@link #ruled()} for lined-paper pages. Encapsulates all page
 * styling so controllers only supply content.
 */
public class DiaryPage extends StackPane {

    /** Height of one ruled line in pixels. */
    public static final int RULE_HEIGHT = 30;

    private static Image ruleTile;

    public DiaryPage(Node content) {
        getStyleClass().add("diary-page");
        setPadding(new Insets(34, 38, 34, 38));
        setAlignment(Pos.TOP_LEFT);
        getChildren().add(content);
        setMinSize(430, 600);
        setPrefSize(520, 660);
        setMaxSize(560, 720);
    }

    /**
     * Adds faint horizontal rules across the content area. The tile is
     * generated in code (no image assets), drawn over the CSS parchment
     * via a mouse-transparent overlay so the parchment gradient is kept.
     */
    public DiaryPage ruled() {
        if (ruleTile == null) {
            ruleTile = makeRuleTile();
        }
        Region lines = new Region();
        lines.setMouseTransparent(true);
        lines.setBackground(new Background(new BackgroundImage(
                ruleTile,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        StackPane.setMargin(lines, new Insets(30, 8, 30, 8));
        getChildren().add(0, lines);
        return this;
    }

    /** Builds a two-page open-book spread with a center spine. */
    public static HBox spread(Node leftPage, Node rightPage) {
        HBox book = new HBox();
        book.getStyleClass().add("book");
        book.setAlignment(Pos.CENTER);
        Region spine = new Region();
        spine.getStyleClass().add("spine");
        book.getChildren().addAll(leftPage, spine, rightPage);
        return book;
    }

    private static Image makeRuleTile() {
        WritableImage tile = new WritableImage(6, RULE_HEIGHT);
        PixelWriter writer = tile.getPixelWriter();
        Color rule = new Color(96 / 255.0, 116 / 255.0, 142 / 255.0, 0.35);
        for (int x = 0; x < 6; x++) {
            writer.setColor(x, RULE_HEIGHT - 1, rule);
        }
        return tile;
    }
}

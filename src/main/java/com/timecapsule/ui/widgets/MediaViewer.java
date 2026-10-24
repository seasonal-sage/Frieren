package com.timecapsule.ui.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.file.Path;

/**
 * A small grimoire window for beholding keepsakes (Module: UI).
 * Photos open large; voice clips and moving pictures open with their own
 * player (play/pause plus a scrub slider). Every window owns its player
 * and lays it to rest on close — inline rows never hold players.
 */
public final class MediaViewer {

    private MediaViewer() {
    }

    /** Opens a photo large in its own window. */
    public static void showPhoto(Path file, String caption) {
        Stage stage = window(caption == null ? "A pressed memory" : caption);
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        Label title = new Label(caption == null ? "Pressed between the pages" : caption);
        title.getStyleClass().add("media-title");
        title.setWrapText(true);

        ImageView view = new ImageView();
        try {
            Image image = new Image(file.toUri().toString(), 1100, 750, true, true, true);
            if (image.isError()) {
                throw new IllegalArgumentException("unreadable image");
            }
            view.setImage(image);
        } catch (Exception ignored) {
            Label faded = new Label("That keepsake has faded — its file is gone.");
            faded.getStyleClass().add("field-error");
            faded.setWrapText(true);
            card.getChildren().addAll(title, faded);
            dress(stage, card);
            stage.show();
            return;
        }
        view.setPreserveRatio(true);
        card.getChildren().addAll(title, view);
        dress(stage, card);
        stage.show();
    }

    /** Opens a voice clip with its own player (autoplays, rests on close). */
    public static void showAudio(Path file, String caption) {
        Stage stage = window(caption == null ? "A voice clip" : caption);
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        Label title = new Label(caption == null ? "A tucked voice clip" : caption);
        title.getStyleClass().add("media-title");
        title.setWrapText(true);
        Label error = new Label("That clip would not sing. Its magic may have faded.");
        error.getStyleClass().add("field-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        Button play = new Button("❚❚");
        play.getStyleClass().add("audio-btn");
        Slider progress = new Slider(0, 100, 0);
        progress.getStyleClass().add("media-slider");
        progress.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progress, Priority.ALWAYS);
        Label time = new Label("0:00 / 0:00");
        time.getStyleClass().add("media-time");
        HBox row = new HBox(10, play, progress, time);
        row.setAlignment(Pos.CENTER);
        card.getChildren().addAll(title, error, row);
        dress(stage, card);

        MediaPlayer player;
        try {
            player = new MediaPlayer(new Media(file.toUri().toString()));
        } catch (Exception e) {
            error.setVisible(true);
            error.setManaged(true);
            play.setDisable(true);
            progress.setDisable(true);
            stage.show();
            return;
        }
        wire(player, play, progress, time, error);
        stage.setOnHidden(e -> rest(player));
        stage.show();
        player.play();
    }

    /** Opens a moving picture with its own screen and player (autoplays, rests on close). */
    public static void showVideo(Path file, String caption) {
        Stage stage = window(caption == null ? "A moving picture" : caption);
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        Label title = new Label(caption == null ? "A moving picture" : caption);
        title.getStyleClass().add("media-title");
        title.setWrapText(true);
        Label error = new Label("That picture would not stir. Its magic may have faded.");
        error.getStyleClass().add("field-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        MediaView screen = new MediaView();
        screen.setPreserveRatio(true);
        screen.setFitWidth(880);
        screen.setFitHeight(500);

        Button play = new Button("❚❚");
        play.getStyleClass().add("audio-btn");
        Slider progress = new Slider(0, 100, 0);
        progress.getStyleClass().add("media-slider");
        progress.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progress, Priority.ALWAYS);
        Label time = new Label("0:00 / 0:00");
        time.getStyleClass().add("media-time");
        HBox row = new HBox(10, play, progress, time);
        row.setAlignment(Pos.CENTER);
        card.getChildren().addAll(title, error, screen, row);
        dress(stage, card);

        MediaPlayer player;
        try {
            player = new MediaPlayer(new Media(file.toUri().toString()));
        } catch (Exception e) {
            error.setVisible(true);
            error.setManaged(true);
            play.setDisable(true);
            progress.setDisable(true);
            stage.show();
            return;
        }
        screen.setMediaPlayer(player);
        wire(player, play, progress, time, error);
        stage.setOnHidden(e -> rest(player));
        stage.show();
        player.play();
    }

    /** Binds play/pause, scrub, clock, and error reporting to a player. */
    private static void wire(MediaPlayer player, Button play, Slider progress,
                             Label time, Label error) {
        player.setOnReady(() -> progress.setMax(player.getTotalDuration().toSeconds()));
        player.currentTimeProperty().addListener((o, old, current) -> {
            if (!progress.isValueChanging()) {
                progress.setValue(current.toSeconds());
            }
            time.setText(fmt(current) + " / " + fmt(player.getTotalDuration()));
        });
        progress.valueProperty().addListener((o, old, value) -> {
            if (progress.isValueChanging()) {
                player.seek(Duration.seconds(value.doubleValue()));
            }
        });
        play.setOnAction(e -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                play.setText("▶");
            } else {
                player.play();
                play.setText("❚❚");
            }
        });
        player.setOnEndOfMedia(() -> {
            player.stop();
            play.setText("▶");
            progress.setValue(0);
        });
        player.setOnError(() -> {
            error.setVisible(true);
            error.setManaged(true);
            play.setText("▶");
        });
    }

    /** Lays a player to rest (stop + dispose, best effort). */
    private static void rest(MediaPlayer player) {
        try {
            player.stop();
            player.dispose();
        } catch (Exception ignored) {
            // Already gone.
        }
    }

    private static Stage window(String caption) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle(caption);
        return stage;
    }

    private static void dress(Stage stage, VBox card) {
        card.getStyleClass().add("media-card");
        card.setPadding(new Insets(16));
        Scene scene = new Scene(card);
        scene.getStylesheets().add(
                MediaViewer.class.getResource("/com/timecapsule/css/diary.css").toExternalForm());
        stage.setScene(scene);
    }

    private static String fmt(Duration moment) {
        long seconds = Math.max(0, (long) moment.toSeconds());
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}

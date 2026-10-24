package com.timecapsule.ui;

import com.timecapsule.ui.widgets.CandleCluster;
import com.timecapsule.ui.widgets.DiaryPage;
import com.timecapsule.ui.widgets.FairyChat;
import com.timecapsule.ui.widgets.FairyGuide;
import com.timecapsule.ai.OllamaFairyProvider;
import com.timecapsule.audit.AuditLogger;
import com.timecapsule.auth.AuthException;
import com.timecapsule.auth.AuthService;
import com.timecapsule.auth.User;
import com.timecapsule.core.JournalException;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.scheduler.CapsuleScheduler;
import com.timecapsule.storage.UserDao;
import com.timecapsule.ui.widgets.WoodTexture;
import com.timecapsule.audio.SoundService;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.List;

/**
 * Application entry point: the desk, the closed grimoire, and the demo
 * contents spread. Controllers + FXML replace these demo pages
 * as Day 2-6 modules land; the navigator and page skin stay.
 */
public class BookApp extends Application {

    public static final int MIN_W = 1280;
    public static final int MIN_H = 800;

    private BookNavigator navigator;
    private FairyGuide frieren;
    private FairyChat fairyChat;
    private EntriesView entriesView;
    private BackCoverView backCover;
    private CapsulesView capsulesView;
    private CalendarView calendarView;
    private HabitsView habitsView;
    private StatsView statsView;
    private CapsuleScheduler capsuleScheduler;
    private final OllamaFairyProvider fairyBrain = new OllamaFairyProvider();

    @Override
    public void start(Stage stage) {
        loadFonts();
        SoundService.instance().loadAll();

        StackPane desk = new StackPane();
        desk.getStyleClass().add("desk");

        // Generated plank-wood tile under everything (offline, ships in jar).
        Region wood = new Region();
        wood.setMouseTransparent(true);
        wood.setBackground(new Background(new BackgroundImage(
                WoodTexture.plankTile(),
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));

        // Warm candlelight pool; below the book so pages stay fully readable.
        Region lamplight = new Region();
        lamplight.getStyleClass().add("lamplight");
        lamplight.setMouseTransparent(true);

        navigator = new BookNavigator();
        navigator.show(buildClosedBook());
        PageTracker.set("cover");

        // Book center-stage, candle cluster resting on the desk to the right.
        VBox candleCol = new VBox(new CandleCluster());
        candleCol.setAlignment(Pos.BOTTOM_CENTER);
        candleCol.setPadding(new Insets(0, 0, 60, 0));
        HBox row = new HBox(70, navigator, candleCol);
        row.setAlignment(Pos.CENTER);

        desk.getChildren().addAll(wood, lamplight, row);

        // Frieren docks bottom-left (chat perches above her) and greets on the cover.
        frieren = new FairyGuide();
        fairyChat = new FairyChat(fairyBrain);
        frieren.setOnTalkRequest(fairyChat::open);
        VBox fairyDock = new VBox(8, fairyChat, frieren);
        fairyDock.setAlignment(Pos.BOTTOM_LEFT);
        fairyDock.setPickOnBounds(false);
        StackPane.setAlignment(fairyDock, Pos.BOTTOM_LEFT);
        StackPane.setMargin(fairyDock, new Insets(0, 0, 10, 24));
        desk.getChildren().add(fairyDock);
        // Cover greeting arrives off-thread: first model load must not freeze launch.
        Task<String> greet = new Task<>() {
            @Override
            protected String call() {
                return fairyBrain.lineFor("cover");
            }
        };
        greet.setOnSucceeded(e -> {
            frieren.say(greet.getValue());
            SoundService.instance().playFrierenGreet();
        });
        greet.setOnFailed(e -> frieren.say(new StaticFairyLines().lineFor("cover")));
        Thread greeter = new Thread(greet, "frieren-greet");
        greeter.setDaemon(true);
        greeter.start();

        entriesView = new EntriesView(navigator, frieren, fairyBrain, stage, this::buildContentsSpread);
        backCover = new BackCoverView(navigator, frieren, fairyBrain, this::buildContentsSpread);
        capsulesView = new CapsulesView(navigator, frieren, fairyBrain, this::buildContentsSpread, entriesView);
        calendarView = new CalendarView(navigator, frieren, fairyBrain, this::buildContentsSpread, entriesView);
        habitsView = new HabitsView(navigator, frieren, fairyBrain, this::buildContentsSpread);
        statsView = new StatsView(navigator, frieren, fairyBrain, this::buildContentsSpread);

        // The seal-watcher: daemon thread, announces through Frieren.
        capsuleScheduler = new CapsuleScheduler(() -> {
            try {
                return JournalManager.listDueEntries();
            } catch (JournalException ex) {
                return List.of(); // No keeper signed in — nothing can be due.
            }
        }, java.time.Duration.ofSeconds(30));
        capsuleScheduler.addListener(memory ->
                Platform.runLater(() -> {
                    frieren.announce("A wax seal has cracked — \""
                            + memory.getTitle() + "\" lies open. Go and read it.");
                    entriesView.refreshIfShowing();
                }));
        capsuleScheduler.start();

        Scene scene = new Scene(desk, MIN_W, MIN_H);
        scene.getStylesheets().add(
                getClass().getResource("/com/timecapsule/css/diary.css").toExternalForm());

        stage.setTitle("Time Capsule — A Personal Journal");
        stage.setMinWidth(MIN_W);
        stage.setMinHeight(MIN_H);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (capsuleScheduler != null) {
            capsuleScheduler.stop();
        }
    }

    /** Closed black grimoire, standing alone in the middle of the desk. */
    private StackPane buildClosedBook() {
        VBox coverBox = new VBox(16);
        coverBox.setAlignment(Pos.CENTER);
        Label title = new Label("Time Capsule");
        title.getStyleClass().add("silver-title");
        Label sub = new Label("a grimoire of memories, kept under lock & key");
        sub.getStyleClass().add("cover-sub");

        TextField nameField = new TextField();
        nameField.setPromptText("keeper name");
        nameField.getStyleClass().add("cover-field");
        nameField.setMaxWidth(300);

        PasswordField secretField = new PasswordField();
        secretField.setPromptText("secret word");
        secretField.getStyleClass().add("cover-field");
        secretField.setMaxWidth(300);

        Label error = new Label();
        error.getStyleClass().add("cover-error");
        error.setWrapText(true);
        error.setMaxWidth(340);

        Button clasp = new Button();
        clasp.getStyleClass().add("clasp-btn");
        clasp.setDefaultButton(true);
        Button swap = new Button();
        swap.getStyleClass().add("cover-link");

        // First keeper ever claims the book; everyone after opens it.
        boolean[] registerMode = {false};
        try {
            registerMode[0] = new UserDao().count() == 0;
        } catch (Exception ignored) {
            // Stay in login mode; the clasp reports database errors.
        }
        Runnable refreshMode = () -> {
            clasp.setText(registerMode[0] ? "Claim the grimoire" : "Open the grimoire");
            swap.setText(registerMode[0] ? "Already bound? Speak your name" : "New keeper? Claim these pages");
            error.setText("");
        };
        refreshMode.run();
        swap.setOnAction(e -> {
            registerMode[0] = !registerMode[0];
            refreshMode.run();
        });
        secretField.setOnAction(e -> clasp.fire());
        coverBox.getChildren().addAll(title, sub, nameField, secretField, error, clasp, swap);

        DiaryPage cover = new DiaryPage(coverBox);
        cover.getStyleClass().add("leather");
        cover.setAlignment(Pos.CENTER);
        cover.setPrefSize(560, 700);

        Region spine = new Region();
        spine.getStyleClass().add("book-spine-left");
        Region edge = new Region();
        edge.getStyleClass().add("page-edge");

        HBox book = new HBox(spine, cover, edge);
        book.getStyleClass().add("book");
        book.setAlignment(Pos.CENTER);

        clasp.setOnAction(e -> {
            SoundService.instance().playClick();
            String name = nameField.getText();
            try {
                User keeper = registerMode[0]
                        ? AuthService.register(name, secretField.getText())
                        : AuthService.login(name, secretField.getText());
                secretField.clear();
                error.setText("");
                fairyChat.loadForCurrentUser();
                navigator.show(buildContentsSpread(), BookNavigator.Direction.FORWARD);
                PageTracker.set("contents");
                List<Memory> opened = capsuleScheduler.checkNow();
                if (opened.isEmpty()) {
                frieren.announce("The pages know you now, " + keeper.getUsername()
                        + ". Write — I'll keep the candles lit.");
                }
                // Otherwise the scheduler's announcement speaks instead.
            } catch (AuthException ex) {
                error.setText(ex.getMessage());
                AuditLogger.log(null, "failed " + (registerMode[0] ? "register" : "login")
                        + " for '" + name + "'");
                frieren.announce(fairyBrain.lineFor("denied"));
                shake(book);
            }
        });

        StackPane wrapper = new StackPane(book);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    /** Scolds the book with a quick sideways shudder (wrong secret word). */
    private void shake(javafx.scene.Node node) {
        TranslateTransition shudder = new TranslateTransition(Duration.millis(60), node);
        shudder.setByX(8);
        shudder.setCycleCount(6);
        shudder.setAutoReverse(true);
        shudder.play();
    }

    /** Table of contents spread (dashboard placeholder until Day 6). */
    private StackPane buildContentsSpread() {
        VBox chapters = new VBox(6);
        chapters.setAlignment(Pos.TOP_LEFT);
        Label heading = new Label("Contents");
        heading.getStyleClass().add("page-heading");
        chapters.getChildren().add(heading);
        Button entriesBtn = new Button("Entries");
        entriesBtn.getStyleClass().add("nav-btn");
        entriesBtn.setOnAction(e -> entriesView.showList());
        chapters.getChildren().add(entriesBtn);
        Button capsulesBtn = new Button("Sealed capsules");
        capsulesBtn.getStyleClass().add("nav-btn");
        capsulesBtn.setOnAction(e -> capsulesView.show());
        chapters.getChildren().add(capsulesBtn);
        Button calendarBtn = new Button("Calendar");
        calendarBtn.getStyleClass().add("nav-btn");
        calendarBtn.setOnAction(e -> calendarView.show());
        chapters.getChildren().add(calendarBtn);
        Button habitsBtn = new Button("Habits");
        habitsBtn.getStyleClass().add("nav-btn");
        habitsBtn.setOnAction(e -> habitsView.show());
        chapters.getChildren().add(habitsBtn);
        Button statsBtn = new Button("Stats");
        statsBtn.getStyleClass().add("nav-btn");
        statsBtn.setOnAction(e -> statsView.show());
        chapters.getChildren().add(statsBtn);
        Button soundBtn = new Button(SoundService.instance().isEnabled() ? "♫ Sound on" : "♫ Sound off");
        soundBtn.getStyleClass().add("nav-btn");
        soundBtn.setOnAction(e -> {
            boolean now = !SoundService.instance().isEnabled();
            SoundService.instance().setEnabled(now);
            soundBtn.setText(now ? "♫ Sound on" : "♫ Sound off");
            SoundService.instance().playClick();
        });
        chapters.getChildren().add(soundBtn);
        Button copyBtn = new Button("Copy & notes");
        copyBtn.getStyleClass().add("nav-btn");
        copyBtn.setOnAction(e -> backCover.show());
        chapters.getChildren().add(copyBtn);
        Button back = new Button("←  Back to cover");
        back.getStyleClass().add("nav-btn");
        back.setOnAction(e -> {
            navigator.show(buildClosedBook(), BookNavigator.Direction.BACKWARD);
            PageTracker.set("cover");
            frieren.say(fairyBrain.lineFor("cover"));
        });
        chapters.getChildren().add(back);
        DiaryPage left = new DiaryPage(chapters).ruled();

        VBox right = new VBox(10);
        right.setAlignment(Pos.CENTER);
        Label quote = new Label("“We write to remember.”");
        quote.getStyleClass().addAll("hand-note");
        Label hint = new Label("Sealed capsules and entries will live on these pages.");
        hint.getStyleClass().add("body-soft");
        hint.setWrapText(true);
        right.getChildren().addAll(quote, hint);
        DiaryPage rightPage = new DiaryPage(right);
        rightPage.setAlignment(Pos.CENTER);

        StackPane spread = new StackPane(DiaryPage.spread(left, rightPage));
        spread.setAlignment(Pos.CENTER);
        return spread;
    }

    /** Bundled OFL fonts; falls back to system fonts when absent. */
    private void loadFonts() {
        loadFont("/com/timecapsule/fonts/Caveat-Variable.ttf");
        loadFont("/com/timecapsule/fonts/CrimsonText-Regular.ttf");
        loadFont("/com/timecapsule/fonts/CrimsonText-Italic.ttf");
        loadFont("/com/timecapsule/fonts/CrimsonText-Bold.ttf");
        loadFont("/com/timecapsule/fonts/PirataOne-Regular.ttf");
    }

    private void loadFont(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in != null) {
                Font.loadFont(in, 10);
            }
        } catch (Exception ignored) {
            // CSS font stacks fall back to Segoe Script / Georgia.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

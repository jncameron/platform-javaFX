package sample;
/*
    testing
 */
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.chart.*;

import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Main extends Application {

    private HashMap<KeyCode, Boolean> keys = new HashMap<KeyCode, Boolean>();

    private ArrayList<Node> platforms = new ArrayList<Node>();
    private ArrayList<Node> coins = new ArrayList<>();
    private ArrayList<Node> ladders = new ArrayList<>();

    private Pane appRoot = new Pane();
    private Pane gameRoot = new Pane();
    private Pane uiRoot = new Pane();

    private Node player;
    private Point2D playerVelocity = new Point2D(0, 0);
    private boolean canJump = true;
    private boolean climbing = false;
    private boolean speedBurst = false;
    private int speedBurstCost = -2;
    private int staminaMax = 1000;
    private int stamina = staminaMax;
    private boolean usingStamina = false;


    private int levelWidth;

    private void initContent() {
        //background size and color
        Rectangle bg = new Rectangle(1200, 720);
        bg.setFill(Color.LIGHTBLUE);

        //block size
        levelWidth = LevelData.LEVEL1[0].length() * 60;

        for (int i = 0; i < LevelData.LEVEL1.length; i++) {
            String line = LevelData.LEVEL1[i];
            for (int j = 0; j < line.length(); j++) {
                switch (line.charAt(j)) {
                    case '0':

                        break;
                    case '1':
                        Node platform = createEntity(j*60, i*60, 60, 60, Color.DARKGREEN);
                        platforms.add(platform);
                        break;
                    case '2':
                        Node coin = createEntity(j*60, i*60, 30, 30, Color.GOLD);
                        coins.add(coin);
                        break;
                    case '3':
                        Node ladder = createEntity(j*60, i*60, 60, 60, Color.BROWN);
                        ladders.add(ladder);
                        break;
                }
            }
        }

        player = createEntity(0, 600, 40, 40, Color.BLUE);

        //scrolling player across screen while moving
        player.translateXProperty().addListener((abs, old, newValue) -> {
            int offset = newValue.intValue();

            if(offset > 640 && offset < levelWidth - 640) {
                gameRoot.setLayoutX(-(offset - 640));
            }
        });

        appRoot.getChildren().addAll(bg, gameRoot, uiRoot);
    }

    private void update() {
        System.out.printf("speedBurst %b, usingStamina %b, stamina %d\n", speedBurst, usingStamina, stamina);


        if (isPressed(KeyCode.SPACE) && player.getTranslateY() >= 5) {
            jumpPlayer();
        }

        //SpeedBurst doubles player speed
        if (isPressed(KeyCode.J) && player.getTranslateY() >= 5) {
            speedBurst = true;
            usingStamina = true;
        }
        if (!isPressed(KeyCode.J)) {
            speedBurst = false;
            usingStamina = false;
        }

        if (isPressed(KeyCode.A) && player.getTranslateX() >= 5) {
            movePlayerX(-5);
        }
        if (isPressed(KeyCode.D) && player.getTranslateX() + 40 <= levelWidth - 5) {
            movePlayerX(5);
        }

        if (isPressed(KeyCode.W)) {
            climbPlayerUp();
        }

        if (isPressed(KeyCode.S)) {
            climbPlayerDown();
        }

        //gravity
        if (playerVelocity.getY() < 10) {
            playerVelocity = playerVelocity.add(0, 1);
        }

        //velocity if not climbing ladder

        if (!isClimbing()) {
            movePlayerY((int)playerVelocity.getY());
        }


        for (Node coin : coins) {
            if(player.getBoundsInParent().intersects(coin.getBoundsInParent())) {
                coin.getProperties().put("alive", false);
            }
        }

        for (Iterator<Node> it = coins.iterator(); it.hasNext(); ) {
            Node coin = it.next();
            if (!(Boolean)coin.getProperties().get("alive")) {
                it.remove();
                gameRoot.getChildren().remove(coin);
            }
        }
        // Regenerate stamina
        spendStamina(10);
    }


    private void spendStamina(int cost) {
        // If stamina is currently being used, any requests to regenerate it should be blocked.
        if (!usingStamina || cost < 0) {
            stamina += cost;
        }
        // Sets min/max for stamina values.
        if (stamina < 0) stamina = 0;
        if (stamina > staminaMax) stamina = staminaMax;
    }

    private void movePlayerX(int value) {
        boolean movingRight = value > 0;

        for (int i = 0; i < Math.abs(value); i++) {
            for (Node platform : platforms) {
                if (player.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                    if (movingRight) {
                        if (player.getTranslateX() + 40 >= platform.getTranslateX()) {
                            return;
                        }
                    }
                    else {
                        if (player.getTranslateX() == platform.getTranslateX() + 60) {
                            return;
                        }
                    }
                }
            }
            // speedBurst: player speeds up time, granting faster movement.
            if (speedBurst && stamina > 0) {
                spendStamina(speedBurstCost);
                player.setTranslateX(player.getTranslateX() + (movingRight ? 2 : -2));
            }
            player.setTranslateX(player.getTranslateX() + (movingRight ? 1 : -1));
        }
    }

    private void movePlayerY(int value) {
        boolean movingDown = value > 0;
        boolean movingUp = value < 0;

        for (int i = 0; i < Math.abs(value); i++) {
            for (Node platform : platforms) {
                if (player.getBoundsInParent().intersects(platform.getBoundsInParent())) {
                    if (movingDown) {

                        if (player.getTranslateY() + 40 == platform.getTranslateY()) {
                            player.setTranslateY(player.getTranslateY() - 1);
                            canJump = true;
                            return;
                        }
                    }

                    else if (movingUp) {

                        if (player.getTranslateY() - 60 == platform.getTranslateY()) {

                            //restart gravity
                            //stop player one pixel below overhead object
                            playerVelocity = playerVelocity.add(0, 1);
                            player.setTranslateY(player.getTranslateY() + 1);
                            canJump = false;
                            return;
                        }
                    }


                    else {
                        if (player.getTranslateY() == platform.getTranslateX() + 60) {
                            return;
                        }
                    }

                }
            }
            player.setTranslateY(player.getTranslateY() + (movingDown ? 1 : -1));
        }
    }


    private void climbPlayerUp() {
        for (Node ladder : ladders) {
            if (player.getBoundsInParent().intersects(ladder.getBoundsInParent())) {
                    climbing = true;
                    //player.setTranslateY(player.getTranslateY() - 1);
                    movePlayerY(-5);
            }
        }
    }

    private void climbPlayerDown() {
        for (Node ladder : ladders) {
            if (player.getBoundsInParent().intersects(ladder.getBoundsInParent())) {
                climbing = true;
                //player.setTranslateY(player.getTranslateY() - 1);
                movePlayerY(5);
            }
        }
    }

    private boolean isClimbing() {
        for (Node ladder : ladders) {
            if (player.getBoundsInParent().intersects(ladder.getBoundsInParent())) {
                return true;
            }
        }
        return false;
    }

    private void jumpPlayer() {
        //no double jump

        if (canJump) {
            playerVelocity = playerVelocity.add(0, -30);
            canJump = false;
        }
    }

    private void bigJumpPlayer() {

        if (canJump) {
            playerVelocity = playerVelocity.add(0, -35);
            canJump = false;
        }
    }

    private Node createEntity(int x, int y, int w, int h, Color color) {
        Rectangle entity = new Rectangle(w, h);
        entity.setTranslateX(x);
        entity.setTranslateY(y);
        entity.setFill(color);
        entity.getProperties().put("alive", true);

        gameRoot.getChildren().add(entity);
        return entity;
    }

    private boolean isPressed(KeyCode key) {
        return keys.getOrDefault(key, false);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        initContent();

        final NumberAxis xAxis = new NumberAxis();
        Scene scene = new Scene(appRoot);
        scene.setOnKeyPressed(event -> keys.put(event.getCode(), true));
        scene.setOnKeyReleased(event -> keys.put(event.getCode(), false));
        primaryStage.setTitle("Platformer");
        primaryStage.setScene(scene);
        primaryStage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                    update();

            }
        };
        timer.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

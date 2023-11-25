package brickGame;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class Main extends Application implements EventHandler<KeyEvent>, GameEngine.OnAction {

    private int level = 1;
    private double xBreak = 0.0f;
    private double centerBreakX;
    private double yBreak = 640.0f;

    private final int breakWidth     = 130;
    private final int breakHeight    = 30;
    private int halfBreakWidth = breakWidth / 2;

    private final int sceneWidth = 500;
    private final int sceneHeight = 700;

    private static int LEFT  = 1;
    private static int RIGHT = 2;

    private Circle ball;
    private double xBall;
    private double yBall;

    private boolean isGoldStatus = false;
    private boolean isExistHeartBlock = false;

    private Rectangle rect;
    private int ballRadius = 10;

    private int destroyedBlockCount = 0;

    private int  heart    = 3;
    private int  score    = 0;
    private long time     = 0;
    private long hitTime  = 0;
    private long goldTime = 0;

    private double v = 1;

    private GameEngine engine;
    public static String savePath    = "D:/save/save.mdds";
    public static String savePathDir = "D:/save/";

    private boolean isGameStarted = false;

    private ArrayList<Block> blocks = new ArrayList<Block>();
    private ArrayList<Bonus> chocolate = new ArrayList<Bonus>();
    private Color[]          colors = new Color[]{
            Color.MAGENTA,
            Color.RED,
            Color.GOLD,
            Color.CORAL,
            Color.AQUA,
            Color.VIOLET,
            Color.GREENYELLOW,
            Color.ORANGE,
            Color.PINK,
            Color.SLATEGREY,
            Color.YELLOW,
            Color.TOMATO,
            Color.TAN,
    };
    public  Pane             root;
    private Label            scoreLabel;
    private Label            heartLabel;
    private Label            levelLabel;

    private boolean loadFromSave = false;

    private VBox pauseMenu;

    Stage  primaryStage;
    Button load    = null;
    Button newGame = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        root = new Pane();
        createPauseMenu();
        setUpScene();
        engine = new GameEngine();
        engine.setOnAction(this);
        engine.setFps(120);

        initBall();
        initBreak();
        initBoard();

        load = new Button("Load Game");
        newGame = new Button("Press [Space Bar] to Start");
        load.setTranslateX(220);
        load.setTranslateY(300);
        newGame.setTranslateX(180);
        newGame.setTranslateY(340);
        scoreLabel = new Label("Score: " + score);
        levelLabel = new Label("Level: " + level);
        levelLabel.setTranslateY(20);
        heartLabel = new Label("Heart : " + heart);
        heartLabel.setTranslateX(sceneWidth - 70);

        root.getChildren().addAll(pauseMenu, rect, ball, scoreLabel, heartLabel, levelLabel, newGame);

        for (Block block : blocks) {
            root.getChildren().add(block.rect);
        }
        //LOAD GAME
        load.setOnAction(event -> {
                loadGame();
                load.setVisible(false);
                newGame.setVisible(false);
        });
        //NEW GAME
        newGame.setOnAction(event -> {
            engine = new GameEngine();
            engine.setOnAction(Main.this);
            engine.setFps(120);
            engine.start();
            load.setVisible(false);
            newGame.setVisible(false);
            isGameStarted = true;
        });
    }

    private void nextLevelScene() {
        Platform.runLater(() -> {
            level++;
            if (level > 1) {
                new Score().showMessage("Level Up :)", this);
            }
            if (level == 10) {
                new Score().showWin(this);
            }
            blocks.clear();
            initBoard();
            for (Block block : blocks) {
                root.getChildren().add(block.rect);
            }
            engine.start();
        });
    }

    private void loadScene(){
        scoreLabel = new Label("Score: " + score);
        levelLabel = new Label("Level: " + level);
        levelLabel.setTranslateY(20);
        heartLabel = new Label("Heart : " + heart);
        heartLabel.setTranslateX(sceneWidth - 70);
        root.getChildren().addAll(rect, ball, scoreLabel, heartLabel, levelLabel, pauseMenu);

        engine.start();
        loadFromSave = false;
    }
    private void setUpScene(){
        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        scene.getStylesheets().add("style.css");
        scene.setOnKeyPressed(this);

        primaryStage.setTitle("Game");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    //***PAUSE  MENU//
    private void createPauseMenu() {
        pauseMenu = new VBox(20); // Spacing of 10 between elements

        Button resumeButton = new Button("Resume");
        Button restartButton = new Button("Restart");
        Button saveButton = new Button("Save Game");
        Button exitButton = new Button("Exit");

        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.setTranslateX(sceneWidth - 280);
        pauseMenu.setTranslateY(sceneHeight - 400);

        resumeButton.setOnAction(event -> togglePause());
        restartButton.setOnAction(event -> restartGame());
        saveButton.setOnAction(event -> saveGame());
        exitButton.setOnAction(event -> exitGame());

        pauseMenu.getChildren().addAll(resumeButton, restartButton, saveButton, exitButton);
        pauseMenu.setVisible(false); // Hide the pause menu initially
    }

    private void exitGame() {
        if (!engine.isStopped()) {
            engine.stop();
            Platform.exit();
        }
    }




    private void togglePause() {
        // Check if the engine is running before toggling the pause state
        if (isGameStarted) {
            boolean isPaused = !pauseMenu.isVisible();
            pauseMenu.setVisible(isPaused);

            if (isPaused) {
                engine.stop();
            } else {
                engine.start();
            }
        }
    }


    //#filled empty space with normal blocks by changing if r % 5==0 to respond to normal blocks
    //Initializing board with random number for block type
    private void initBoard() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < level + 1; j++) {
                int r = new Random().nextInt(500);
                int type;
                if (r % 5 == 0) {
                    type = Block.BLOCK_NORMAL;
                }
                if (r % 10 == 1) {
                    type = Block.BLOCK_CHOCO;
                } else if (r % 10 == 2) {
                    if (!isExistHeartBlock) {
                        type = Block.BLOCK_HEART;
                        isExistHeartBlock = true;
                    } else {
                        type = Block.BLOCK_NORMAL;
                    }
                } else if (r % 10 == 3) {
                    type = Block.BLOCK_STAR;
                } else {
                    type = Block.BLOCK_NORMAL;
                }
                blocks.add(new Block(j, i, colors[r % (colors.length)], type));
                //System.out.println("colors " + r % (colors.length));
            }
        }
    }



    public static void main(String[] args) {
        launch(args);
    }




    //Keyboard Input
    @Override
    public void handle(KeyEvent event) {
        switch (event.getCode()) {
            case LEFT:
                move(LEFT);
                break;
            case RIGHT:

                move(RIGHT);
                break;
            case DOWN:
                //setPhysicsToBall();
                break;
            case S:
                saveGame();
                break;
            case ESCAPE:
                togglePause();
                break;
            case P:
                togglePause();
                break;
        }
    }


    private void move(final int direction) {
        final int frames = 30; // Number of frames in the animation
        KeyFrame keyFrame = new KeyFrame(Duration.millis(4), e -> {
            if (xBreak == (sceneWidth - breakWidth) && direction == RIGHT) {
                return;
            }
            if (xBreak == 0 && direction == LEFT) {
                return;
            }
            if (direction == RIGHT) {
                xBreak++;
            } else {
                xBreak--;
            }
            centerBreakX = xBreak + halfBreakWidth;
        });

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(frames);
        timeline.play();
    }
//    private void initBall() {
//        Random random = new Random();
//        xBall = random.nextInt(sceneWidth) + 1;
//        yBall = random.nextInt(sceneHeight - 200) + ((level + 1) * Block.getHeight()) + 15;
//        ball = new Circle();
//        ball.setRadius(ballRadius);
//        ball.setFill(new ImagePattern(new Image("ball.png")));
//    }

    private void initBall() {
        xBall = xBreak + (breakWidth / 2);

        yBall = yBreak - ballRadius - 10;

        ball = new Circle();
        ball.setRadius(ballRadius);
        ball.setFill(new ImagePattern(new Image("ball.png")));

        goDownBall = false; // Assuming goDownBall = true means moving down
    }

    private void resetBall() {
        xBall = xBreak + (breakWidth / 2);
        yBall = yBreak - ballRadius - 10;
        goDownBall = false;
    }


    private void initBreak() {
        rect = new Rectangle();
        rect.setWidth(breakWidth);
        rect.setHeight(breakHeight);
        rect.setX(xBreak);
        rect.setY(yBreak);

        ImagePattern pattern = new ImagePattern(new Image("block.jpg"));

        rect.setFill(pattern);
    }


    private boolean goDownBall                  = true;
    private boolean goRightBall                 = true;
    private boolean collideToBreak = false;
    private boolean collideToBreakAndMoveToRight = true;
    private boolean collideToRightWall = false;
    private boolean collideToLeftWall = false;
    private boolean collideToRightBlock = false;
    private boolean collideToBottomBlock = false;
    private boolean collideToLeftBlock = false;
    private boolean collideToTopBlock = false;

    private double vX = 2.000;
    private double vY = 2.000;


    private void resetCollideFlags() {

        collideToBreak = false;
        collideToBreakAndMoveToRight = false;
        collideToRightWall = false;
        collideToLeftWall = false;

        collideToRightBlock = false;
        collideToBottomBlock = false;
        collideToLeftBlock = false;
        collideToTopBlock = false;
    }

    private void setPhysicsToBall() {
        //v = ((time - hitTime) / 1000.000) + 1.000;

        v = 1.000;
        if (goDownBall) {
            yBall += vY; //down
        } else {
            yBall -= vY; //up
        }


        if (goRightBall) {
            xBall += vX; //right
        } else {
            xBall -= vX; //left
        }

        //Collision with top boundary
        if (yBall <= 0) {
            //vX = 1.000;
            resetCollideFlags();
            goDownBall = true;
            return;
        }

        //Collision with bottom boundary
        if (yBall >= sceneHeight) {
            goDownBall = false;
            if (!isGoldStatus) {
                //TODO game over
                heart--;
                new Score().show(sceneWidth / 2, sceneHeight / 2, -1, this);

                if (heart == 0) {
                    new Score().showGameOver(this);
                    engine.stop();
                }

            }
            //return;
        }

        //Collision with paddle
        if (yBall >= yBreak - ballRadius) {
            //System.out.println("Collide1");
            if (xBall >= xBreak && xBall <= xBreak + breakWidth) {
                hitTime = time;
                resetCollideFlags();
                collideToBreak = true;
                goDownBall = false;

                double relation = (xBall - centerBreakX) / (breakWidth / 2);

                //ball direction when collision with paddle
                if (Math.abs(relation) <= 0.3) {
                    //vX = 0;
                    vX = Math.abs(relation);
                } else if (Math.abs(relation) > 0.3 && Math.abs(relation) <= 0.7) {
                    vX = (Math.abs(relation) * 1.2);
                    //System.out.println("vX " + vX);
                } else {
                    vX = (Math.abs(relation) * 1.5);
                    //System.out.println("vX " + vX);
                }

                if (xBall - centerBreakX > 0) {
                    collideToBreakAndMoveToRight = true;
                } else {
                    collideToBreakAndMoveToRight = false;
                }
                //System.out.println("Collide2");
            }
        }

        if (xBall >= sceneWidth) {
            resetCollideFlags();
            collideToRightWall = true;
        }

        if (xBall <= 0) {
            resetCollideFlags();
            collideToLeftWall = true;
        }

        if (collideToBreak) {
            if (collideToBreakAndMoveToRight) {
                goRightBall = true;
            } else {
                goRightBall = false;
            }
        }

        //Wall Collide

        if (collideToRightWall) {
            goRightBall = false;
        }

        if (collideToLeftWall) {
            goRightBall = true;
        }

        //Block Collide

        if (collideToRightBlock) {
            goRightBall = true;
        }

        if (collideToLeftBlock) {
            goRightBall = false;
        }

        if (collideToTopBlock) {
            goDownBall = false;
        }

        if (collideToBottomBlock) {
            goDownBall = true;
        }


    }


    private void checkDestroyedCount() {
        if (destroyedBlockCount == blocks.size()) {
            //TODO win level todo...
            //System.out.println("You Win");

            nextLevel();
        }
    }

    private void saveGame() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new File(savePathDir).mkdirs();
                File file = new File(savePath);
                ObjectOutputStream outputStream = null;
                try {
                    outputStream = new ObjectOutputStream(new FileOutputStream(file));

                    outputStream.writeInt(level);
                    outputStream.writeInt(score);
                    outputStream.writeInt(heart);
                    outputStream.writeInt(destroyedBlockCount);


                    outputStream.writeDouble(xBall);
                    outputStream.writeDouble(yBall);
                    outputStream.writeDouble(xBreak);
                    outputStream.writeDouble(yBreak);
                    outputStream.writeDouble(centerBreakX);
                    outputStream.writeLong(time);
                    outputStream.writeLong(goldTime);
                    outputStream.writeDouble(vX);


                    outputStream.writeBoolean(isExistHeartBlock);
                    outputStream.writeBoolean(isGoldStatus);
                    outputStream.writeBoolean(goDownBall);
                    outputStream.writeBoolean(goRightBall);
                    outputStream.writeBoolean(collideToBreak);
                    outputStream.writeBoolean(collideToBreakAndMoveToRight);
                    outputStream.writeBoolean(collideToRightWall);
                    outputStream.writeBoolean(collideToLeftWall);
                    outputStream.writeBoolean(collideToRightBlock);
                    outputStream.writeBoolean(collideToBottomBlock);
                    outputStream.writeBoolean(collideToLeftBlock);
                    outputStream.writeBoolean(collideToTopBlock);

                    ArrayList<BlockSerializable> blockSerializables = new ArrayList<BlockSerializable>();
                    for (Block block : blocks) {
                        if (block.isDestroyed) {
                            continue;
                        }
                        blockSerializables.add(new BlockSerializable(block.row, block.column, block.type));
                    }

                    outputStream.writeObject(blockSerializables);


                    new Score().showMessage("Game Saved", Main.this);



                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    private void loadGame() {

        LoadSave loadSave = new LoadSave();
        loadSave.read();


        isExistHeartBlock = loadSave.isExistHeartBlock;
        isGoldStatus = loadSave.isGoldStauts;
        goDownBall = loadSave.goDownBall;
        goRightBall = loadSave.goRightBall;
        collideToBreak = loadSave.colideToBreak;
        collideToBreakAndMoveToRight = loadSave.colideToBreakAndMoveToRight;
        collideToRightWall = loadSave.colideToRightWall;
        collideToLeftWall = loadSave.colideToLeftWall;
        collideToRightBlock = loadSave.colideToRightBlock;
        collideToBottomBlock = loadSave.colideToBottomBlock;
        collideToLeftBlock = loadSave.colideToLeftBlock;
        collideToTopBlock = loadSave.colideToTopBlock;
        level = loadSave.level;
        score = loadSave.score;
        heart = loadSave.heart;
        destroyedBlockCount = loadSave.destroyedBlockCount;
        xBall = loadSave.xBall;
        yBall = loadSave.yBall;
        xBreak = loadSave.xBreak;
        yBreak = loadSave.yBreak;
        centerBreakX = loadSave.centerBreakX;
        time = loadSave.time;
        goldTime = loadSave.goldTime;
        vX = loadSave.vX;

        blocks.clear();
        chocolate.clear();

        for (BlockSerializable ser : loadSave.blocks) {
            int r = new Random().nextInt(200);
            blocks.add(new Block(ser.row, ser.j, colors[r % colors.length], ser.type));
        }


        try {
            loadFromSave = true;
            loadScene();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void nextLevel() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    vX = 1.000;

                    engine.stop();
                    resetCollideFlags();
                    goDownBall = true;

                    isGoldStatus = false;
                    isExistHeartBlock = false;

                    hitTime = 0;
                    time = 0;
                    goldTime = 0;

                    blocks.clear();
                    chocolate.clear();
                    destroyedBlockCount = 0;
                    resetBall();
                    nextLevelScene();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void restartGame() {

        try {
            engine.stop();
            blocks.clear();
            resetCollideFlags();
            level = 1;
            heart = 3;
            score = 0;
            destroyedBlockCount = 0;
            initBoard();
            initBall();
            resetBall();
            start(primaryStage);

//            destroyedBlockCount = 0;
//            resetCollideFlags();
//            goDownBall = true;
//
//            isGoldStatus = false;
//            isExistHeartBlock = false;
//            hitTime = 0;
//            time = 0;
//            goldTime = 0;
//
//            blocks.clear();
//            chocolate.clear();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onUpdate() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                levelLabel.setText("Level: " + level);
                scoreLabel.setText("Score: " + score);
                heartLabel.setText("Heart : " + heart);

                rect.setX(xBreak);
                rect.setY(yBreak);
                ball.setCenterX(xBall);
                ball.setCenterY(yBall);

                for (Bonus choco : chocolate) {
                    choco.choco.setY(choco.y);
                }
            }
        });

        if (yBall  >= Block.getPaddingTop() && yBall <= (Block.getHeight() * (level + 1)) + Block.getPaddingTop()) {
            for (final Block block : blocks) {
                int hitCode = block.checkHitToBlock(xBall, yBall, ballRadius);
                if (hitCode != Block.NO_HIT) {
                    score += 1;
                    System.out.println(hitCode);
                    new Score().show(block.x, block.y, 1, this);

                    block.rect.setVisible(false);
                    block.isDestroyed = true;
                    destroyedBlockCount++;
                    //System.out.println("size is " + blocks.size());
                    resetCollideFlags();

                    if (block.type == Block.BLOCK_CHOCO) {
                        final Bonus choco = new Bonus(block.row, block.column);
                        choco.timeCreated = time;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                root.getChildren().add(choco.choco);
                            }
                        });
                        chocolate.add(choco);
                    }

                    if (block.type == Block.BLOCK_STAR) {
                        goldTime = time;
                        ball.setFill(new ImagePattern(new Image("goldball.png")));
                        System.out.println("gold ball");
                        root.getStyleClass().add("goldRoot");
                        isGoldStatus = true;
                    }

                    if (block.type == Block.BLOCK_HEART) {
                        heart++;
                    }

                    if (hitCode == Block.HIT_RIGHT) {
                        collideToRightBlock = true;
                    } else if (hitCode == Block.HIT_BOTTOM) {
                        collideToBottomBlock = true;
                    } else if (hitCode == Block.HIT_LEFT) {
                        collideToLeftBlock = true;
                    } else if (hitCode == Block.HIT_TOP) {
                        collideToTopBlock = true;
                    }

                }

                //TODO hit to break and some work here....
                //System.out.println("Break in row:" + block.row + " and column:" + block.column + " hit");
            }
        }
    }


    @Override
    public void onInit() {

    }

    @Override
    public void onPhysicsUpdate() {
        checkDestroyedCount();
        setPhysicsToBall();


        if (time - goldTime > 5000) {
            ball.setFill(new ImagePattern(new Image("ball.png")));
            root.getStyleClass().remove("goldRoot");
            isGoldStatus = false;
        }

        for (Bonus choco : chocolate) {
            if (choco.y > sceneHeight || choco.taken) {
                continue;
            }
            if (choco.y >= yBreak && choco.y <= yBreak + breakHeight && choco.x >= xBreak && choco.x <= xBreak + breakWidth) {
                System.out.println("You Got it and +3 score for you");
                choco.taken = true;
                choco.choco.setVisible(false);
                score += 3;
                new Score().show(choco.x, choco.y, 3, this);
            }
            choco.y += ((time - choco.timeCreated) / 1000.000) + 1.000;
        }

        //System.out.println("time is:" + time + " goldTime is " + goldTime);

    }


    @Override
    public void onTime(long time) {
        this.time = time;
    }
}

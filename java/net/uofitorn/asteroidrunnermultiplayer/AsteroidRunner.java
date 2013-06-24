package net.uofitorn.asteroidrunnermultiplayer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.media.SoundPool;
import android.os.Handler;

import android.graphics.Color;

import java.util.Timer;
import java.util.TimerTask;

public class AsteroidRunner {

    private final static String TAG = "AsteroidRunner";

    public static final int DELAY_TO_START_GAME = 3;

    public static final int GAMESTATE_PLAYING = 0;
    public static final int GAMESTATE_WON_GAME = 1;
    public static final int GAMESTATE_CRASHED = 2;
    public static final int GAMESTATE_MAIN_MENU = 3;
    public static final int GAMESTATE_IN_LOBBY = 4;
    public static final int GAMESTATE_WAITING = 5;
    public static final int GAMESTATE_COUNTDOWN = 6;
    public static final int GAMESTATE_LOST_GAME =7;

    public static final int COMMAND_CONNECT = 0;
    public static final int COMMAND_DISCONNECT = 1;
    public static final int COMMAND_BEGIN = 2;
    public static final int COMMAND_LISTEN = 3;
    public static final int COMMAND_MOVE_DOWN = 10;
    public static final int COMMAND_MOVE_UP = 11;
    public static final int COMMAND_MOVE_LEFT = 12;
    public static final int COMMAND_MOVE_RIGHT = 13;
    public static final int COMMAND_PLAYER_WON = 20;
    public static final int COMMAND_OTHER_PLAYER_CRASHED = 21;

    public static final int OTHER_PLAYER_STATE_CRASHED = 0;
    public static final int OTHER_PLAYER_STATE_ALIVE = 1;

    private int explosionSound;
    private int startingSound;
    private int gameOverSound;
    private static SoundPool soundPool;

    private int playerX = 0;
    private int playerY = 0;
    private int otherPlayerX = 0;
    private int otherPlayerY = 0;
    private int surroundingMines = 0;
    private int gridSquareLength = 0;
    private int gridSquareHeight = 0;
    private int canvasWidth = 0;
    private int canvasHeight = 0;
    private int gameState;
    private int otherPlayerState;
    private int frameBufferWidth;
    private int frameBufferHeight;
    private int countDown;

    public static final double DIFFICULTY_SUPER_EASY_VALUE = 0.02;
    public static final double DIFFICULTY_EASY_VALUE = 0.08;
    public static final double DIFFICULTY_MEDIUM_VALUE = 0.13;
    public static final double DIFFICULTY_HARD_VALUE = 0.18;
    public static final int DIFFICULTY_SUPER_EASY = 0;
    public static final int DIFFICULTY_EASY = 1;
    public static final int DIFFICULTY_MEDIUM = 2;
    public static final int DIFFICULTY_HARD = 3;

    private static final int boardSize = 12;
    private int[][] gameBoard = new int[boardSize][boardSize];
    private int[][] playerVisited = new int[boardSize][boardSize];
    private int[][] otherPlayerVisited = new int[boardSize][boardSize];
    //private double difficultyLevelValue = DIFFICULTY_MEDIUM_VALUE;
    //private int difficultyLevel = DIFFICULTY_MEDIUM;
    private double difficultyLevelValue = DIFFICULTY_EASY_VALUE;
    private int difficultyLevel = DIFFICULTY_EASY;

    private Bitmap backgroundImage;
    private Bitmap playerShip;
    private Bitmap otherPlayerShip;
    private Bitmap spaceMine;
    private Bitmap squareBG, visitedSquare;
    private Bitmap explosion;
    private Bitmap gameOver;
    private Bitmap upArrow, downArrow, leftArrow, rightArrow;
    private Bitmap minesNearby;
    private Bitmap wormhole;
    private Bitmap newGame;
    private Bitmap youEscaped;
    private Bitmap redStar;
    private Bitmap startScreen, mainMenu;
    private Bitmap difficulty, difficulty0, difficulty1, difficulty2, difficulty3;
    private Bitmap[] numerals = new Bitmap[8];

    int upArrowX = 227, upArrowY = 640;
    int downArrowX = 227, downArrowY = 840;
    int leftArrowX = 130, leftArrowY = 740;
    int rightArrowX =  325, rightArrowY = 740;
    int newGameX = 95, newGameY = 700;
    int mainMenuX = 95, mainMenuY = 800;
    int newGameX2 = 90, newGameY2 = 540;
    int difficultyLabelX = 130, difficultyLabelY = 650;
    int difficultyLevelX = 280, difficultyLevelY = 650;
    float scaleX, scaleY;

    private String status = "";

    private NetworkThread mNetworkThread;
    boolean connected = false;
    boolean sentPlayerWonMessage = false;

    class MyTask extends TimerTask {
        public void run() {
            otherPlayerState = OTHER_PLAYER_STATE_ALIVE;
            otherPlayerX = 0;
            otherPlayerY = 0;
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            int command = bundle.getInt("command");
            switch(command) {
                case COMMAND_BEGIN:
                    setStatus("Beginning game");
                    startNewGame();
                    break;
                case COMMAND_MOVE_DOWN:
                    if (otherPlayerY != boardSize - 1) {
                        otherPlayerY++;
                    }
                    break;
                case COMMAND_MOVE_UP:
                    if (otherPlayerY != 0) {
                        otherPlayerY--;
                    }
                    break;
                case COMMAND_MOVE_LEFT:
                    if (otherPlayerX != 0) {
                        otherPlayerX--;
                    }
                    break;
                case COMMAND_MOVE_RIGHT:
                    if (otherPlayerX != boardSize - 1) {
                        otherPlayerX++;
                    }
                    break;
                case COMMAND_OTHER_PLAYER_CRASHED:
                    otherPlayerState = OTHER_PLAYER_STATE_CRASHED;
                    soundPool.play(explosionSound, 1.0f, 1.0f, 1, 0, 1.0f);
                    MyTask t = new MyTask();
                    new Timer().schedule(t, 3000);
                    break;
                case COMMAND_PLAYER_WON:
                    soundPool.play(gameOverSound, 1.0f, 1.0f, 1, 0, 1.0f);
                    gameState = GAMESTATE_LOST_GAME;
                    break;
            }
        }
    };

    public AsteroidRunner(Context context, int frameBufferWidth, int frameBufferHeight) {
        this.frameBufferWidth = frameBufferWidth;
        this.frameBufferHeight = frameBufferHeight;
        initImages(context);
        initSounds(context);
        do {
            shuffleMap();
        } while (!isSolvable());
        calcSurroundingMines();
        gameState = GAMESTATE_MAIN_MENU;
        playerVisited[0][0] = 1;
    }

    public void initSounds(Context context) {
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
        explosionSound = soundPool.load(context, R.raw.explosiongameover, 1);
        startingSound = soundPool.load(context, R.raw.startingsound, 1);
        gameOverSound = soundPool.load(context, R.raw.gameover, 1);
    }

    public void initImages(Context context) {
        Resources res = context.getResources();
        backgroundImage = BitmapFactory.decodeResource(res, R.drawable.starbackground);
        playerShip = BitmapFactory.decodeResource(res, R.drawable.lander_plain);
        spaceMine = BitmapFactory.decodeResource(res, R.drawable.spacemine);
        squareBG = BitmapFactory.decodeResource(res, R.drawable.bgsquare);
        explosion = BitmapFactory.decodeResource(res, R.drawable.explosion);
        gameOver = BitmapFactory.decodeResource(res, R.drawable.gameover);
        upArrow = BitmapFactory.decodeResource(res, R.drawable.uparrow);
        downArrow = BitmapFactory.decodeResource(res, R.drawable.downarrow);
        leftArrow = BitmapFactory.decodeResource(res, R.drawable.leftarrow);
        rightArrow = BitmapFactory.decodeResource(res, R.drawable.rightarrow);
        minesNearby = BitmapFactory.decodeResource(res, R.drawable.minesnearby);
        visitedSquare = BitmapFactory.decodeResource(res, R.drawable.visitedsquare);
        wormhole = BitmapFactory.decodeResource(res, R.drawable.wormhole);
        newGame = BitmapFactory.decodeResource(res, R.drawable.newgame);
        youEscaped = BitmapFactory.decodeResource(res, R.drawable.youescaped);
        redStar = BitmapFactory.decodeResource(res, R.drawable.redstar);
        startScreen = BitmapFactory.decodeResource(res, R.drawable.startscreen);
        difficulty = BitmapFactory.decodeResource(res, R.drawable.difficulty);
        difficulty0 = BitmapFactory.decodeResource(res, R.drawable.difficulty0);
        difficulty1 = BitmapFactory.decodeResource(res, R.drawable.difficulty1);
        difficulty2 = BitmapFactory.decodeResource(res, R.drawable.difficulty2);
        difficulty3 = BitmapFactory.decodeResource(res, R.drawable.difficulty3);
        mainMenu = BitmapFactory.decodeResource(res, R.drawable.mainmenu);
        otherPlayerShip = BitmapFactory.decodeResource(res, R.drawable.otherplayership);

        numerals[0] = BitmapFactory.decodeResource(res, R.drawable.zero);
        numerals[1] = BitmapFactory.decodeResource(res, R.drawable.one);
        numerals[2] = BitmapFactory.decodeResource(res, R.drawable.two);
        numerals[3] = BitmapFactory.decodeResource(res, R.drawable.three);
        numerals[4] = BitmapFactory.decodeResource(res, R.drawable.four);
        numerals[5] = BitmapFactory.decodeResource(res, R.drawable.five);
        numerals[6] = BitmapFactory.decodeResource(res, R.drawable.six);
        numerals[7] = BitmapFactory.decodeResource(res, R.drawable.seven);
    }

    public void startNewGame() {
        playerX = 0;
        playerY = 0;
        do {
            shuffleMap();
        } while (!isSolvable());

        calcSurroundingMines();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                playerVisited[i][j] = 0;
            }
        }

        for (int i = DELAY_TO_START_GAME; i != 0; i--) {
            try {
                setStatus("Starting game in " + i);
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e(TAG, "Caught exception: " + e + " + in Thread.sleep");
            }
        }
        playerVisited[0][0] = 1;
        gameState = GAMESTATE_PLAYING;
        Log.i(TAG, "Playing start sound in startNewGame()");
        soundPool.play(startingSound, 1.0f, 1.0f, 1, 0, 1f);
        otherPlayerState = OTHER_PLAYER_STATE_ALIVE;
    }

    public void startNewMultiplayerGame() {
        setStatus("Connecting to server...");
        gameState = GAMESTATE_IN_LOBBY;
        gameState = GAMESTATE_WAITING;
        connected = true;
        mNetworkThread = new NetworkThread(handler);
        mNetworkThread.start();
    }

    public void drawMines(Canvas canvas) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (gameBoard[i][j] == 1) {
                    canvas.drawBitmap(spaceMine, i * gridSquareLength, j * gridSquareHeight, null);
                }
            }
        }
    }

    public void drawSquareCover(Canvas canvas) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (playerVisited[i][j] == 0) {
                    if (i == 0 && j == 0)
                        continue;
                    if (i == boardSize - 1 && j == boardSize - 1)
                        continue;
                    canvas.drawBitmap(squareBG, i * gridSquareLength, j * gridSquareHeight, null);
                }
            }
        }
    }

    public void drawVisited(Canvas canvas) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (playerVisited[i][j] == 1) {
                    if (i == boardSize - 1 && j == boardSize - 1)
                        continue;
                    if (i == playerX && j == playerY)
                        continue;
                    canvas.drawBitmap(visitedSquare, i * gridSquareLength, j * gridSquareHeight, null);
                }
            }
        }
    }

    public void drawBackground(Canvas canvas) {
        if (gameState == GAMESTATE_PLAYING) {
            canvas.drawBitmap(backgroundImage, 0, 0, null);
            Paint gridPaint = new Paint();
            gridPaint.setAntiAlias(true);
            gridPaint.setARGB(255, 255, 255, 255);
            for (int i = 0; i <= boardSize; i++) {
                canvas.drawLine(0, i * gridSquareHeight, frameBufferWidth, i * gridSquareHeight, gridPaint);
            }
            for (int i = 0; i < boardSize; i++) {
                canvas.drawLine(i * gridSquareLength, 0, i * gridSquareLength, frameBufferWidth, gridPaint);
            }
            canvas.drawBitmap(wormhole, 11 * gridSquareLength, 11 * gridSquareLength, null);
            canvas.drawBitmap(redStar, 0, 0, null);
        } else if (gameState == GAMESTATE_MAIN_MENU) {
            canvas.drawBitmap(startScreen, 0, 0, null);
            canvas.drawBitmap(newGame, newGameX2, newGameY2, null);
            /* canvas.drawBitmap(difficulty, difficultyLabelX, difficultyLabelY, null);
            switch (difficultyLevel) {
                case DIFFICULTY_SUPER_EASY:
                    canvas.drawBitmap(difficulty0, difficultyLevelX, difficultyLevelY, null);
                    break;
                case DIFFICULTY_EASY:
                    canvas.drawBitmap(difficulty1, difficultyLevelX, difficultyLevelY, null);
                    break;
                case DIFFICULTY_MEDIUM:
                    canvas.drawBitmap(difficulty2, difficultyLevelX, difficultyLevelY, null);
                    break;
                case DIFFICULTY_HARD:
                    canvas.drawBitmap(difficulty3, difficultyLevelX, difficultyLevelY, null);
                    break;
            } */
        } else if (gameState == GAMESTATE_WON_GAME) {
            canvas.drawBitmap(backgroundImage, 0, 0, null);
            Paint gridPaint = new Paint();
            gridPaint.setAntiAlias(true);
            gridPaint.setARGB(255, 255, 255, 255);
            for (int i = 0; i <= boardSize; i++) {
                canvas.drawLine(0, i * gridSquareHeight, frameBufferWidth, i * gridSquareHeight, gridPaint);
            }
            for (int i = 0; i < boardSize; i++) {
                canvas.drawLine(i * gridSquareLength, 0, i * gridSquareLength, frameBufferWidth, gridPaint);
            }
            canvas.drawBitmap(wormhole, 11 * gridSquareLength, 11 * gridSquareLength, null);
            canvas.drawBitmap(redStar, 0, 0, null);
        }
    }

    public void drawResetGame(Canvas canvas) {
        canvas.drawBitmap(backgroundImage, 0, 0, null);
        Paint gridPaint = new Paint();
        gridPaint.setAntiAlias(true);
        gridPaint.setARGB(255, 255, 255, 255);
        for (int i = 0; i <= boardSize; i++) {
            canvas.drawLine(0, i * gridSquareHeight, frameBufferWidth, i * gridSquareHeight, gridPaint);
        }
        for (int i = 0; i < boardSize; i++) {
            canvas.drawLine(i * gridSquareLength, 0, i * gridSquareLength, frameBufferWidth, gridPaint);
        }
        canvas.drawBitmap(wormhole, 11 * gridSquareLength, 11 * gridSquareLength, null);
        canvas.drawBitmap(redStar, 0, 0, null);
        canvas.drawBitmap(explosion, playerX * gridSquareLength, playerY * gridSquareHeight, null);
    }

    public void drawLobby(Canvas canvas) {
        canvas.drawBitmap(startScreen, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setTextSize(28);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(status, canvas.getWidth() / 2, canvas.getHeight() - 100, paint);
    }

    public void drawPlayerShip(Canvas canvas) {
        canvas.drawBitmap(playerShip, playerX * gridSquareLength, playerY * gridSquareHeight + 5, null);
    }

    public void drawOtherPlayerShip(Canvas canvas) {
        if(otherPlayerState == OTHER_PLAYER_STATE_ALIVE)
            canvas.drawBitmap(otherPlayerShip, otherPlayerX * gridSquareLength, otherPlayerY * gridSquareHeight + 5, null);
        else
            canvas.drawBitmap(explosion, otherPlayerX * gridSquareLength, otherPlayerY * gridSquareHeight, null);
    }

    public void drawMineCount(Canvas canvas) {
        canvas.drawBitmap(minesNearby, 10, canvasWidth + 20, null);
        canvas.drawBitmap(numerals[surroundingMines], 390, frameBufferWidth - 60, null);
    }

    public void calcSurroundingMines() {
        int numMines = 0;
        if (playerX != 0 && playerY != 0 && (gameBoard[playerX - 1][playerY - 1] == 1))
            numMines++;
        if (playerY != 0 && (gameBoard[playerX][playerY - 1] == 1))
            numMines++;
        if (playerY != 0 && (playerX != boardSize - 1) && (gameBoard[playerX + 1][playerY - 1] == 1))
            numMines++;
        if ((playerX != boardSize - 1) && (gameBoard[playerX + 1][playerY] == 1))
            numMines++;
        if ((playerX != boardSize - 1) && (playerY != boardSize - 1) && (gameBoard[playerX + 1][playerY + 1] == 1))
            numMines++;
        if ((playerY != boardSize - 1) && (gameBoard[playerX][playerY + 1] == 1))
            numMines++;
        if (playerX != 0 && (playerY != boardSize - 1) && (gameBoard[playerX - 1][playerY + 1] == 1))
            numMines++;
        if (playerX != 0 && (gameBoard[playerX - 1][playerY] == 1))
            numMines++;
        surroundingMines = numMines;
    }

    public void handleMoveUp() {
        if (playerY != 0) {
            playerY--;
            playerVisited[playerX][playerY] = 1;
            mNetworkThread.sendMessage(COMMAND_MOVE_UP);
        }
    }

    public void handleMoveDown() {
        if (playerY != boardSize - 1) {
            playerY++;
            playerVisited[playerX][playerY] = 1;
            mNetworkThread.sendMessage(COMMAND_MOVE_DOWN);
        }
    }

    public void handleMoveRight() {
        if (playerX != boardSize - 1) {
            playerX++;
            playerVisited[playerX][playerY] = 1;
            mNetworkThread.sendMessage(COMMAND_MOVE_RIGHT);
        }
    }

    public void handleMoveLeft() {
        if (playerX != 0) {
            playerX--;
            playerVisited[playerX][playerY] = 1;
            mNetworkThread.sendMessage(COMMAND_MOVE_LEFT);
        }
    }

    public void resetGame() {
        playerX = 0;
        playerY = 0;
        calcSurroundingMines();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                playerVisited[i][j] = 0;
            }
        }
        playerVisited[0][0] = 1;
        gameState = GAMESTATE_PLAYING;
        soundPool.play(startingSound, 1.0f, 1.0f, 1, 0, 1f);
    }

    public void calculateCollision() {
        if (gameBoard[playerX][playerY] == 1) {
            gameState = GAMESTATE_CRASHED;
            soundPool.play(explosionSound, 1.0f, 1.0f, 1, 0, 1.0f);
            mNetworkThread.sendMessage(COMMAND_OTHER_PLAYER_CRASHED);
        } else if (playerX == 11 && playerY == 11) {
            if (!sentPlayerWonMessage) {
                mNetworkThread.sendMessage(COMMAND_PLAYER_WON);
                gameState = GAMESTATE_WON_GAME;
                sentPlayerWonMessage = true;
            }
        }
    }

    public void drawYouWon(Canvas canvas) {
        canvas.drawBitmap(youEscaped, 10, frameBufferWidth + 50, null);
        canvas.drawBitmap(newGame, newGameX, newGameY, null);
        canvas.drawBitmap(mainMenu, mainMenuX, mainMenuY, null);
    }

    public int getGameState() {
        return gameState;
    }

    private void shuffleMap() {
        double difficultyThreshold = 0;
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                difficultyThreshold = difficultyLevelValue;
                double rand = Math.random();
                if (rand < difficultyThreshold) {
                    gameBoard[i][j] = 1;
                } else {
                    gameBoard[i][j] = 0;
                }
            }
        }
        gameBoard[0][0] = 0;
        gameBoard[boardSize - 1][boardSize - 1] = 0;
    }

    private boolean isSolvable() {
        if (findPath(0, 0))
            return true;
        else
            return false;
    }

    private boolean findPath(int x, int y) {
        if ((x > 11) || (y > 11) || (x < 0) || (y < 0))
            return false;
        if ((x == 11) && (y == 11))
            return true;
        if (gameBoard[x][y] == 1)
            return false;
        if (gameBoard[x][y] == 3)
            return false;

        // Mark that we have visited this square
        gameBoard[x][y] = 3;

        if (findPath(x, y + 1))
            return true;
        if (findPath(x + 1, y))
            return true;
        if (findPath(x, y - 1))
            return true;
        if (findPath(x - 1, y))
            return true;
        return false;
    }

    void initializeBounds(int canvasWidth, int canvasHeight, float scaleX, float scaleY) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        gridSquareLength = canvasWidth / boardSize;
        gridSquareHeight = gridSquareLength;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.newGameX2 = (canvasWidth / 2) - (newGame.getWidth() / 2);
    }

    void initializeImages(int framebufferWidth, int framebufferHeight) {
        squareBG = Bitmap.createScaledBitmap(squareBG, gridSquareLength, gridSquareHeight, true);
        spaceMine = Bitmap.createScaledBitmap(spaceMine, gridSquareLength, gridSquareHeight, true);
        startScreen = Bitmap.createScaledBitmap(startScreen, framebufferWidth, framebufferHeight, true);
        backgroundImage = Bitmap.createScaledBitmap(backgroundImage, framebufferWidth, framebufferHeight, true);
        playerShip = Bitmap.createScaledBitmap(playerShip, gridSquareLength, gridSquareHeight, true);
        explosion = Bitmap.createScaledBitmap(explosion, gridSquareLength, gridSquareHeight, true);
        visitedSquare = Bitmap.createScaledBitmap(visitedSquare, gridSquareLength, gridSquareHeight, true);
        wormhole = Bitmap.createScaledBitmap(wormhole, gridSquareLength, gridSquareHeight, true);
        redStar = Bitmap.createScaledBitmap(redStar, gridSquareLength, gridSquareHeight, true);
        otherPlayerShip = Bitmap.createScaledBitmap(otherPlayerShip, gridSquareLength, gridSquareHeight, true);
    }

    void drawGameOver(Canvas canvas) {
        canvas.drawBitmap(gameOver, 85, frameBufferWidth + 50, null);
        //canvas.drawBitmap(newGame, newGameX, newGameY, null);
        //canvas.drawBitmap(mainMenu, mainMenuX, mainMenuY, null);
    }

    void drawControls(Canvas canvas) {
        canvas.drawBitmap(upArrow, upArrowX, upArrowY, null);
        canvas.drawBitmap(downArrow, downArrowX, downArrowY , null);
        canvas.drawBitmap(leftArrow, leftArrowX, leftArrowY, null);
        canvas.drawBitmap(rightArrow, rightArrowX, rightArrowY, null);
    }

    void processTouchEvent(float touchX, float touchY) {
        if (gameState == GAMESTATE_PLAYING) {
            if (checkBounds(upArrowX, upArrowY, upArrow.getWidth(), upArrow.getHeight(), touchX, touchY)) {
                handleMoveUp();
            } else if (checkBounds(downArrowX, downArrowY, downArrow.getWidth(), downArrow.getHeight(), touchX, touchY)) {
                handleMoveDown();
            } else if (checkBounds(leftArrowX, leftArrowY, leftArrow.getWidth(), leftArrow.getHeight(), touchX, touchY)) {
                handleMoveLeft();
            } else if (checkBounds(rightArrowX, rightArrowY, rightArrow.getWidth(), rightArrow.getHeight(), touchX, touchY)) {
                handleMoveRight();
            }
            calcSurroundingMines();
            calculateCollision();
        } else if(gameState == GAMESTATE_CRASHED) {
          /*  if(checkBounds(newGameX, newGameY, newGame.getWidth(), newGame.getHeight(), touchX, touchY)) {
                startNewGame();
            } else if (checkBounds(mainMenuX, mainMenuY, mainMenu.getWidth(), mainMenu.getHeight(), touchX, touchY)) {
                gameState = GAMESTATE_MAIN_MENU;
            } */
        } else if(gameState == GAMESTATE_WON_GAME) {
            if(checkBounds(newGameX, newGameY, newGame.getWidth(), newGame.getHeight(), touchX, touchY)) {
                startNewGame();
            } else if (checkBounds(mainMenuX, mainMenuY, mainMenu.getWidth(), mainMenu.getHeight(), touchX, touchY)) {
                gameState = GAMESTATE_MAIN_MENU;
            }
        } else if (gameState == GAMESTATE_MAIN_MENU) {
            if(checkBounds(newGameX2, newGameY2, newGame.getWidth(), newGame.getHeight(), touchX, touchY)) {
                startNewMultiplayerGame();
            } /* else if (checkBounds(difficultyLabelX, difficultyLabelY, difficulty.getWidth(), difficulty.getHeight(), touchX, touchY) ||
                    checkBounds(difficultyLevelX, difficultyLevelY, difficulty2.getWidth(), difficulty2.getHeight(), touchX, touchY)) {
                switch (difficultyLevel) {
                    case DIFFICULTY_SUPER_EASY:
                        difficultyLevel = DIFFICULTY_EASY;
                        difficultyLevelValue = DIFFICULTY_EASY_VALUE;
                        break;
                    case DIFFICULTY_EASY:
                        difficultyLevel = DIFFICULTY_MEDIUM;
                        difficultyLevelValue = DIFFICULTY_MEDIUM_VALUE;
                        break;
                    case DIFFICULTY_MEDIUM:
                        difficultyLevel = DIFFICULTY_HARD;
                        difficultyLevelValue = DIFFICULTY_HARD_VALUE;
                        break;
                    case DIFFICULTY_HARD:
                        difficultyLevel = DIFFICULTY_SUPER_EASY;
                        difficultyLevelValue = DIFFICULTY_SUPER_EASY_VALUE;
                        break;
                }
            } */

        }
    }

    boolean checkBounds(int x, int y, float xSize, float ySize, float touchX, float touchY) {
        if ((touchX > x) && (touchX < x + xSize) && (touchY > y) && (touchY < y + ySize))
            return true;
        else
            return false;
    }

    void setStatus(String status) {
        this.status = status;
    }

    void closeConnection() {
        mNetworkThread.closeConnection();
    }
}
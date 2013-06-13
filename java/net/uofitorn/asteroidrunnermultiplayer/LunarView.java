package net.uofitorn.asteroidrunnermultiplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;

public class LunarView extends SurfaceView implements SurfaceHolder.Callback {

    public class LunarThread extends Thread {
        private LunarView panel;
        private static final String TAG = "LunarThread";

        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        public static final int STATE_WIN = 5;
        private int mode = STATE_RUNNING;

        private int canvasHeight = 1;
        private int canvasWidth = 1;
        private int boardWidth = 1;
        private int boardHeight = 1;
        private Handler handler;
        private boolean running = false;
        private SurfaceHolder surfaceHolder;
        private AsteroidRunner asteroidRunner;

        public static final int LEFT_ARROW = 0;
        public static final int RIGHT_ARROW = 1;
        public static final int UP_ARROW = 2;
        public static final int DOWN_ARROW = 3;

        Bitmap framebuffer;
        Bitmap framebufferFinal;
        int frameBufferWidth = 540;
        int frameBufferHeight = 960;
        float scaleX;
        float scaleY;

        private int didDrawCrash = 0;

        Canvas fbCanvas;

        public LunarThread(SurfaceHolder surfaceHolder, Context ctx, Handler handler) {
            super();
            this.surfaceHolder = surfaceHolder;
            this.handler = handler;
            context = ctx;
            framebuffer = Bitmap.createBitmap(frameBufferWidth, frameBufferHeight, Bitmap.Config.RGB_565);
            fbCanvas = new Canvas(framebuffer);
            asteroidRunner = new AsteroidRunner(ctx, frameBufferWidth, frameBufferHeight);
            Log.i(TAG, "Testing logging to lunarthread");
        }

        public void setRunning(boolean running) { //Allow us to stop the thread
            this.running = running;
        }

        public void setState(int mode) {
            synchronized (surfaceHolder) {
                setState(mode, null);
            }
        }

        public void setState(int mode, CharSequence message) {
            synchronized (surfaceHolder) {
                this.mode = mode;
            }
        }

        public void setSurfaceSize(int width, int height) {
            synchronized (surfaceHolder) {
                canvasWidth = width;
                canvasHeight = height;
                boardWidth = width;
                boardHeight = boardWidth;
                scaleX = (float) frameBufferWidth / width;
                scaleY = (float) frameBufferHeight / height;
                asteroidRunner.initializeBounds(frameBufferWidth, frameBufferHeight, scaleX, scaleY);
                asteroidRunner.initializeImages(frameBufferWidth, frameBufferHeight);
            }
        }

        public void closeConnection() {
            asteroidRunner.closeConnection();
        }

        public void unpause() {
            setState(STATE_RUNNING);
        }

        public void pause() {
            Log.i(TAG, "Application is paused");
            synchronized (surfaceHolder) {
                if (mode == STATE_RUNNING)
                    setState(STATE_PAUSE);
            }
        }

        public boolean getRunning() {
            return running;
        }

        public void updateState() {
            asteroidRunner.calculateCollision();
            if ((asteroidRunner.getGameState() == AsteroidRunner.GAMESTATE_CRASHED) && didDrawCrash == 1) {
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    Log.e(TAG, "Caught exception in thread.sleep");
                }
                didDrawCrash = 0;
                asteroidRunner.resetGame();
            }
        }

        private void doDraw(Canvas canvas) {
            Rect dstRect = new Rect();
            if (asteroidRunner.getGameState() == AsteroidRunner.GAMESTATE_PLAYING) {
                asteroidRunner.drawBackground(fbCanvas);
                asteroidRunner.drawMineCount(fbCanvas);
                asteroidRunner.drawSquareCover(fbCanvas);
                asteroidRunner.drawControls(fbCanvas);
                asteroidRunner.drawOtherPlayerShip(fbCanvas);
                asteroidRunner.drawPlayerShip(fbCanvas);
            } else if (asteroidRunner.getGameState() == AsteroidRunner.GAMESTATE_CRASHED) {
                asteroidRunner.drawResetGame(fbCanvas);
                asteroidRunner.drawSquareCover(fbCanvas);
                didDrawCrash = 1;
            } else if (asteroidRunner.getGameState() == AsteroidRunner.GAMESTATE_WON_GAME) {
                asteroidRunner.drawBackground(fbCanvas);
                asteroidRunner.drawMines(fbCanvas);
                asteroidRunner.drawVisited(fbCanvas);
                asteroidRunner.drawYouWon(fbCanvas);
            } else if (asteroidRunner.getGameState() == AsteroidRunner.GAMESTATE_MAIN_MENU) {
                asteroidRunner.drawBackground(fbCanvas);
            } else if (asteroidRunner.getGameState() == AsteroidRunner.GAMESTATE_IN_LOBBY || asteroidRunner.getGameState() == AsteroidRunner.GAMESTATE_WAITING) {
                asteroidRunner.drawLobby(fbCanvas);
            }
            canvas.getClipBounds(dstRect);
            framebufferFinal = Bitmap.createScaledBitmap(framebuffer, canvasWidth, canvasHeight, true);
            canvas.drawBitmap(framebufferFinal, 0, 0, null);
        }

        @Override
        public void run() {
            while (running) {
                Canvas c = null;
                try {
                    c = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                        if(mode == STATE_RUNNING)
                            updateState();
                        if (c != null)
                            doDraw(c);
                    }
                } finally {
                    if (c != null) {
                        surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        void doKeyDown(int keyCode, KeyEvent msg) {
            synchronized (surfaceHolder) {
                if (keyCode == KeyEvent.KEYCODE_W)
                    asteroidRunner.handleMoveUp();
                else if (keyCode == KeyEvent.KEYCODE_S)
                    asteroidRunner.handleMoveDown();
                else if (keyCode == KeyEvent.KEYCODE_A)
                    asteroidRunner.handleMoveLeft();
                else if (keyCode == KeyEvent.KEYCODE_D)
                    asteroidRunner.handleMoveRight();
                asteroidRunner.calcSurroundingMines();
                asteroidRunner.calculateCollision();
            }
        }

        void handleTouchEvent(float x, float y) {
            x = x * scaleX;
            y = y * scaleY;
            asteroidRunner.processTouchEvent(x, y);
        }
    }

    private LunarThread thread;
    private Context context;
    private static final String TAG = "LunarView";

    //public LunarView(Context context, AttributeSet attrs) {
    public LunarView(Context context) {
        //super(context, attrs);
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        thread = new LunarThread(getHolder(), context, new Handler() {
            @Override
            public void handleMessage(Message m) {

            }
        });
    }

    public LunarThread getThread() {
        return thread;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        thread.setSurfaceSize(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //thread.setRunning(true);
        //thread.start();
        if (thread.getState() == Thread.State.TERMINATED) {
            thread = new LunarThread(getHolder(), context, new Handler() {
                @Override
                public void handleMessage(Message m) {

                }
            });
            thread.setRunning(true);
            thread.start();
        }
        else {
            thread.setRunning(true);
            thread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.closeConnection();
        Log.i(TAG, "Closing connection");
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // try again shutting down the thread
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            thread.handleTouchEvent(event.getX(), event.getY());
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        thread.doKeyDown(keyCode, msg);
        return true;
    }

}


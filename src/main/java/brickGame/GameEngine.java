package brickGame;


import javafx.application.Platform;

public class GameEngine {

    private OnAction onAction;
    private int fps = 15;
    private Thread updateThread;
    private Thread physicsThread;
    public boolean isStopped = true;

    private volatile boolean isPaused = false;

    public void setOnAction(OnAction onAction) {
        this.onAction = onAction;
    }

    /**
     * @param fps set fps and we convert it to millisecond
     */
    public void setFps(int fps) {
        this.fps = 1000 / fps;
    }


    public synchronized void pause() {
        isPaused = true;
    }

    public synchronized void resume() {
        isPaused = false;
        notifyAll(); // Wake up the waiting threads
    }
    private void Update() {
        updateThread = new Thread(() -> {
            while (!updateThread.isInterrupted()) {
                synchronized (this) {
                    while (isPaused) {
                        try {
                            wait(); // Wait until resume is called
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return; // Exit the loop if interrupted
                        }
                    }
                }
                Platform.runLater(() -> {
                    if (onAction != null) {
                        onAction.onUpdate(); // Execute game update logic
                    }
                });

                try {
                    Thread.sleep(fps); // Control the update rate
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // Exit the loop if interrupted
                }
            }
        });
        updateThread.start();
    }
    /*private synchronized void Update() {
        updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!updateThread.isInterrupted()) {
                    try {
                        onAction.onUpdate();
                        Thread.sleep(fps);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        updateThread.start();
    }
     */

    private void Initialize() {
        onAction.onInit();
    }

    private void PhysicsCalculation() {
        physicsThread = new Thread(() -> {
            while (!physicsThread.isInterrupted()) {
                synchronized (this) {
                    while (isPaused) {
                        try {
                            wait(); // Wait until resume is called
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return; // Exit the loop if interrupted
                        }
                    }
                }
                Platform.runLater(() -> {
                    if (onAction != null) {
                        onAction.onPhysicsUpdate(); // Execute physics update logic
                    }
                });

                try {
                    Thread.sleep(fps); // Control the update rate
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // Exit the loop if interrupted
                }
            }
        });
        physicsThread.start();
    }


    public void start() {
        time = 0;
        Initialize();
        Update();
        PhysicsCalculation();
        TimeStart();
        isStopped = false;
    }

    public void stop() {
        if (!isStopped) {
            isStopped = true;
            updateThread.stop();
            physicsThread.stop();
            timeThread.stop();
        }
    }

    private long time = 0;

    private Thread timeThread;

    private void TimeStart() {
        timeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        time++;
                        onAction.onTime(time);
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        timeThread.start();
    }


    public interface OnAction {
        void onUpdate();

        void onInit();

        void onPhysicsUpdate();

        void onTime(long time);
    }

}

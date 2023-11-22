package brickGame;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class GameEngine {

    private OnAction onAction;
    private double fps = 15;
    private Timeline updateTimeline;
    private Timeline physicsTimeline;
    private Timeline timeTimeline;
    private boolean isStopped = true;
    private long time = 0;

    public void setOnAction(OnAction onAction) {
        this.onAction = onAction;
    }

    public void setFps(double fps) {
        this.fps = fps;
    }

    private void initializeTimelines() {
        updateTimeline = new Timeline(new KeyFrame(Duration.millis(1000 / fps), e -> {
            if (onAction != null) {
                onAction.onUpdate();
            }
        }));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);

        physicsTimeline = new Timeline(new KeyFrame(Duration.millis(1000 / fps), e -> {
            if (onAction != null) {
                onAction.onPhysicsUpdate();
            }
        }));
        physicsTimeline.setCycleCount(Timeline.INDEFINITE);

        timeTimeline = new Timeline(new KeyFrame(Duration.millis(1), e -> {
            time++;
            onAction.onTime(time);
        }));
        timeTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void start() {
        if (isStopped) {
            time = 0;
            initializeTimelines();
            onAction.onInit();
            updateTimeline.play();
            physicsTimeline.play();
            timeTimeline.play();
            isStopped = false;
        }
    }

    public void stop() {
        if (!isStopped) {
            isStopped = true;
            updateTimeline.stop();
            physicsTimeline.stop();
            timeTimeline.stop();
        }
    }

//    public void pause() {
//        updateTimeline.pause();
//        physicsTimeline.pause();
//        timeTimeline.pause();
//    }
//
//    public void resume() {
//        updateTimeline.play();
//        physicsTimeline.play();
//        timeTimeline.play();
//    }

    public interface OnAction {
        void onUpdate();

        void onInit();

        void onPhysicsUpdate();

        void onTime(long time);
    }
}

package org.fxmisc.richtext.skin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;

import com.sun.javafx.binding.ExpressionHelper;

final class BooleanPulse extends BooleanExpression {

    private ExpressionHelper<Boolean> listenerHelper;
    private final Timeline timeline;
    private boolean currentValue;

    public BooleanPulse(Duration duration) {
        this(duration, true);
    }

    public BooleanPulse(Duration duration, boolean initialValue) {
        currentValue = initialValue;

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(new KeyFrame(duration, event -> toggle()));
    }

    public void start() {
        timeline.play();
    }

    public void start(boolean initialValue) {
        if(currentValue != initialValue)
            toggle();
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }

    public void stop(boolean terminalValue) {
        timeline.stop();
        if(currentValue != terminalValue)
            toggle();
    }

    private void toggle() {
        currentValue = !currentValue;
        ExpressionHelper.fireValueChangedEvent(listenerHelper);
    }

    @Override
    public boolean get() {
        return currentValue;
    }

    @Override
    public void addListener(ChangeListener<? super Boolean> listener) {
        listenerHelper = ExpressionHelper.addListener(listenerHelper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Boolean> listener) {
        listenerHelper = ExpressionHelper.removeListener(listenerHelper, listener);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listenerHelper = ExpressionHelper.addListener(listenerHelper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listenerHelper = ExpressionHelper.removeListener(listenerHelper, listener);
    }
}

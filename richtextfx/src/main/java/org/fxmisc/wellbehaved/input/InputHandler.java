package org.fxmisc.wellbehaved.input;

import javafx.event.EventHandler;
import javafx.scene.input.InputEvent;

public abstract class InputHandler implements EventHandler<InputEvent> {

    static final InputHandler EMPTY = new InputHandler() {

        @Override
        public void handle(InputEvent event) {
            // do nothing
        }

        @Override
        InputReceiver getTarget() {
            return null;
        }

        @Override
        InputHandler orElse(InputHandler other) {
            return other;
        }

        @Override
        NonEmptyInputHandler overrideBy(NonEmptyInputHandler other) {
            return other;
        }

        @Override
        InputHandler without(InputHandler subHandler) {
            return this;
        }

        @Override
        boolean isEqualTo(InputHandler other) {
            return other == this;
        }
    };

    // package-private constructor to prevent subclassing by users
    InputHandler() {}

    abstract InputReceiver getTarget();

    abstract InputHandler orElse(InputHandler other);

    abstract NonEmptyInputHandler overrideBy(NonEmptyInputHandler other);

    abstract InputHandler without(InputHandler subHandler);

    abstract boolean isEqualTo(InputHandler other);

    public void remove() {
        InputHandler oldHandler = getTarget().getOnInput();
        getTarget().setOnInput(oldHandler.without(this));
    }
}

abstract class NonEmptyInputHandler extends InputHandler {

    @Override
    final InputHandler orElse(InputHandler other) {
        return other.overrideBy(this);
    }

    @Override
    final NonEmptyInputHandler overrideBy(NonEmptyInputHandler other) {
        return new CompositeInputHandler(other, this);
    }
}

class CompositeInputHandler extends NonEmptyInputHandler {
    private final NonEmptyInputHandler first;
    private final NonEmptyInputHandler second;

    CompositeInputHandler(NonEmptyInputHandler first, NonEmptyInputHandler second) {
        if(first.getTarget() != second.getTarget()) {
            throw new IllegalArgumentException("Cannot compose input handlers for different targets.\n"
                    + "Target 1: " + first.getTarget() + "\n"
                    + "Target 2: " + second.getTarget());
        }
        this.first = first;
        this.second = second;
    }

    @Override
    public void handle(InputEvent event) {
        first.handle(event);
        if(!event.isConsumed()) {
            second.handle(event);
        }
    }

    @Override
    InputReceiver getTarget() {
        return first.getTarget();
    }

    @Override
    InputHandler without(InputHandler subHandler) {
        if(this.isEqualTo(subHandler)) {
            return EMPTY;
        } else {
            InputHandler a = first.without(subHandler);
            InputHandler b = second.without(subHandler);
            return (a == first && b == second)
                    ? this
                    : a.orElse(b);
        }
    }

    @Override
    boolean isEqualTo(InputHandler other) {
        if(other instanceof CompositeInputHandler) {
            CompositeInputHandler that = (CompositeInputHandler) other;
            return first.isEqualTo(that.first) && second.isEqualTo(that.second);
        } else {
            return false;
        }
    }
}
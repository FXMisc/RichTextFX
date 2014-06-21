package org.fxmisc.flowless;

import javafx.beans.value.ObservableDoubleValue;

public interface Virtualized {
    ObservableDoubleValue totalWidthEstimateProperty();
    ObservableDoubleValue totalHeightEstimateProperty();

    ObservableDoubleValue horizontalPositionProperty();
    ObservableDoubleValue verticalPositionProperty();
    default double getHorizontalPosition() { return horizontalPositionProperty().get(); }
    default double getVerticalPosition() { return verticalPositionProperty().get(); }

    void setHorizontalPosition(double pos);
    void setVerticalPosition(double pos);

    void scrollHorizontally(double deltaX);
    void scrollVertically(double deltaY);
}

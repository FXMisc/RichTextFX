package org.fxmisc.richtext.model;

/**
 * Interface to the segment type enums.
 * 
 * Each segment type is defined by a specific enum instance.
 * Access to the enum types should always go through this interface
 * so that users can extend the supported types by specifying additional enums.
 * The default segment types are defined in {@link DefaultSegmentTypes}. 
 */
public interface SegmentType {

    /**
     * @return The name of the segment type. 
     *         This is usually different from the all-capital enum instance name.
     */
    String getName();
}

package org.fxmisc.richtext.model;

/**
 * Base class for a {@link TextOps} implementation that uses a text-based segment
 *
 * @param <SEG> the type of segment
 * @param <S> the type of segment style
 */
public abstract class TextOpsBase<SEG, S> extends SegmentOpsBase<SEG, S> implements TextOps<SEG, S> {

    TextOpsBase(SEG empty) {
        super(empty);
    }
}

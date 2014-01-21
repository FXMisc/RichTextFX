package codearea.control;

public class SequenceChange<S> {

    protected final int position;
    protected final S removed;
    protected final S inserted;

    public SequenceChange(int position, S removed, S inserted) {
        this.position = position;
        this.removed = removed;
        this.inserted = inserted;
    }

    public int getPosition() { return position; };
    public S getRemoved() { return removed; }
    public S getInserted() { return inserted; }

}

package codearea.control;

public class StringChange {

    private final int position;
    private final String removed;
    private final String inserted;

    public StringChange(int position, String removed, String inserted) {
        this.position = position;
        this.removed = removed;
        this.inserted = inserted;
    }

    public int getPosition() { return position; };
    public String getRemoved() { return removed; }
    public String getInserted() { return inserted; }
}

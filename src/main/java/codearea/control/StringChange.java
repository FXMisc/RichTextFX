package codearea.control;

import java.util.Optional;

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

    /**
     * Merges this change with the given change, if possible.
     * This change is considered to be the former and the given
     * change is considered to be the latter.
     * Changes can be merged if either
     * <ul>
     *   <li>the latter's start matches the former's added text end; or</li>
     *   <li>the latter's removed text end matches the former's added text end.</li>
     * </ul>
     * @param latter change to merge with this change.
     * @return a new merged change if changes can be merged,
     * {@code null} otherwise.
     */
    public Optional<StringChange> mergeWith(StringChange latter) {
        if(latter.position == this.position + this.inserted.length()) {
            String removedText = this.removed + latter.removed;
            String addedText = this.inserted + latter.inserted;
            return Optional.of(new StringChange(this.position, removedText, addedText));
        }
        else if(latter.position + latter.removed.length() == this.position + this.inserted.length()) {
            if(this.position <= latter.position) {
                String addedText = this.inserted.substring(0, latter.position - this.position) + latter.inserted;
                return Optional.of(new StringChange(this.position, this.removed, addedText));
            }
            else {
                String removedText = latter.removed.substring(0, this.position - latter.position) + this.removed;
                return Optional.of(new StringChange(latter.position, removedText, latter.inserted));
            }
        }
        else {
            return Optional.empty();
        }
    }
}

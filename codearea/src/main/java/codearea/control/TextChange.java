package codearea.control;

import java.util.Optional;

public class TextChange extends SequenceChange<String> {

    public TextChange(int position, String removed, String inserted) {
        super(position, removed, inserted);
    }

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
    public Optional<TextChange> mergeWith(TextChange latter) {
        if(latter.position == this.position + this.inserted.length()) {
            String removedText = this.removed + latter.removed;
            String addedText = this.inserted + latter.inserted;
            return Optional.of(new TextChange(this.position, removedText, addedText));
        }
        else if(latter.position + latter.removed.length() == this.position + this.inserted.length()) {
            if(this.position <= latter.position) {
                String addedText = this.inserted.substring(0, latter.position - this.position) + latter.inserted;
                return Optional.of(new TextChange(this.position, this.removed, addedText));
            }
            else {
                String removedText = latter.removed.substring(0, this.position - latter.position) + this.removed;
                return Optional.of(new TextChange(latter.position, removedText, latter.inserted));
            }
        }
        else {
            return Optional.empty();
        }
    }
}

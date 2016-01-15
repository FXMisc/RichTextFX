package org.fxmisc.richtext;

public class RichTextChange<S, PS> extends TextChange<StyledDocument<S, PS>, RichTextChange<S, PS>> {

    public RichTextChange(int position, StyledDocument<S, PS> removed, StyledDocument<S, PS> inserted) {
        super(position, removed, inserted);
        System.out.println("\n");
        System.out.println(System.nanoTime());
        System.out.println("Checking removed doc");
        checkDocPars(removed);
        System.out.println("Checking inserted doc");
        checkDocPars(inserted);
    }

    @Override
    protected final StyledDocument<S, PS> concat(StyledDocument<S, PS> a, StyledDocument<S, PS> b) {
        return a.concat(b);
    }

    @Override
    protected final StyledDocument<S, PS> sub(StyledDocument<S, PS> doc, int from, int to) {
        return doc.subSequence(from, to);
    }

    @Override
    protected final RichTextChange<S, PS> create(int position, StyledDocument<S, PS> removed, StyledDocument<S, PS> inserted) {
        return new RichTextChange<>(position, removed, inserted);
    }

    private void checkDocPars(StyledDocument<S, PS> doc) {
        doc.getParagraphs().stream().forEach(x -> {
            boolean shouldBeEmptyPar = x.length() == 0;
            if (shouldBeEmptyPar) {
                if (x.getClass().getSimpleName().equals("NormalParagraph")) {
                    throw new IllegalStateException("An Empty Paragraph cannot be a NormalParagraph");
                }
            }
        });
    }
}

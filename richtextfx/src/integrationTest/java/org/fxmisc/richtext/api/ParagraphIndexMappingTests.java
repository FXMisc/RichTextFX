package org.fxmisc.richtext.api;

import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.TextBuildingUtils;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class ParagraphIndexMappingTests extends InlineCssTextAreaAppTest {

    private static final int TOTAL_NUMBER_OF_LINES = 80;
    private static final int LAST_PAR_INDEX = TOTAL_NUMBER_OF_LINES - 1;
    private static final String CONTENT = TextBuildingUtils.buildLines(TOTAL_NUMBER_OF_LINES);

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        area.replaceText(CONTENT);
    }

    @Test
    public void all_par_to_visible_par_index_is_correct() {
        interact(() -> area.showParagraphAtTop(0));
        assertEquals(Optional.of(0), area.allParToVisibleParIndex(0));

        interact(() -> area.showParagraphAtBottom(LAST_PAR_INDEX));
        assertEquals(Optional.of(area.getVisibleParagraphs().size() - 1), area.allParToVisibleParIndex(LAST_PAR_INDEX));
    }

    @Test
    public void all_par_to_visible_par_index_after_replace() {
        interact(() -> {
            area.clear();
            area.replaceText( "123\nabc" );
        });

        interact(() -> area.replaceText( "123\nxyz" ));

        interact(() -> {
            assertEquals(Optional.of(1), area.allParToVisibleParIndex(1));
            assertEquals(Optional.of(0), area.allParToVisibleParIndex(0));
        });
    }

    @Test
    public void visible_par_to_all_par_index_is_correct() {
        interact(() -> area.showParagraphAtTop(0));
        assertEquals(0, area.visibleParToAllParIndex(0));

        interact(() -> area.showParagraphAtBottom(LAST_PAR_INDEX));
        assertEquals(LAST_PAR_INDEX, area.visibleParToAllParIndex(area.getVisibleParagraphs().size() - 1));

    }
}

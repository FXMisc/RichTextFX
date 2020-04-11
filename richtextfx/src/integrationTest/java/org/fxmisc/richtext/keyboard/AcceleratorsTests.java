package org.fxmisc.richtext.keyboard;

import javafx.event.EventTarget;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AcceleratorsTests extends InlineCssTextAreaAppTest {

	@Test
	public void typing_alt_control_combinations_dont_consume_events_if_they_dont_have_any_character_assigned() {


		interact(() -> {

			AcceleratorsTestsHelper[] helpers = {
					//CHARACTER WITHOUT MODIFIERS
					new AcceleratorsTestsHelper(area, "f", KeyCode.F, false, false, true),
					//CHARACTER WITH CONTROL
					new AcceleratorsTestsHelper(area, "", KeyCode.F, true, false, false),
					//CHARACTER WITH ALT
					new AcceleratorsTestsHelper(area, "", KeyCode.F, false, true, false),
					//CHARACTER WITH ALT + CONTROL / ALTGR on Windows
					new AcceleratorsTestsHelper(area, "", KeyCode.F, true, true, false),
					//ALT + CONTROL / ALTGR on Windows with an assigned special character (E -> Euro)
					new AcceleratorsTestsHelper(area, "€", KeyCode.E, true, true, true)
			};
			for (AcceleratorsTestsHelper helper : helpers) {
				area.fireEvent(helper.keyEvent);
				assertEquals(helper.expectedConsumeResult, helper.keyEvent.isConsumed());
			}
		});
	}

	private static class AcceleratorsTestsHelper {

		KeyEvent keyEvent;
		boolean expectedConsumeResult;

		public AcceleratorsTestsHelper(EventTarget source, String character, KeyCode key, boolean controlDown, boolean altDown, boolean expected) {
			keyEvent = new KeyEvent(source, source, KeyEvent.KEY_TYPED, character, key.getName(), key,
					false, controlDown, altDown, false);
			expectedConsumeResult = expected;
		}
	}
}
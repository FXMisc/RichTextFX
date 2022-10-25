package org.fxmisc.richtext;

import javafx.event.EventTarget;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

//This class requires to be in this package, as it requires access to GenericStyledAreaBehavior.
public class AcceleratorsTests extends InlineCssTextAreaAppTest {

	@Test
	public void typing_alt_control_combinations_dont_consume_events_if_they_dont_have_any_character_assigned() {
		AcceleratorsTestsHelper[] events;

		if (System.getProperty("os.name").startsWith("Windows")) {
			//WINDOWS TESTS
			events = new AcceleratorsTestsHelper[]{
					//CHARACTER WITHOUT MODIFIERS
					new AcceleratorsTestsHelper(area, "f", KeyCode.F, false, false, true),
					//CHARACTER WITH CONTROL
					new AcceleratorsTestsHelper(area, "\0", KeyCode.F, true, false, false),
					//CHARACTER WITH ALT
					new AcceleratorsTestsHelper(area, "\0", KeyCode.F, false, true, false),
					//CHARACTER WITH ALT + CONTROL / ALTGR on Windows
					new AcceleratorsTestsHelper(area, "\0", KeyCode.K, true, true, false),
					//ALT + CONTROL / ALTGR on Windows with an assigned special character (E -> Euro on spanish keyboard)
					new AcceleratorsTestsHelper(area, "\u20AC", KeyCode.E, true, true, true)
			};
		} else {
			//LINUX TESTS
			events = new AcceleratorsTestsHelper[]{
					//CHARACTER WITHOUT MODIFIERS
					new AcceleratorsTestsHelper(area, "f", KeyCode.F, false, false, true),
					//CHARACTER WITH CONTROL
					new AcceleratorsTestsHelper(area, "\0", KeyCode.F, true, false, false),
					//CHARACTER WITH ALT
					new AcceleratorsTestsHelper(area, "\0", KeyCode.F, false, true, false),
					//CHARACTER WITH ALT + CONTROL
					new AcceleratorsTestsHelper(area, "\0", KeyCode.F, true, true, false),
			};
		}
		AcceleratorsTestsHelper helper;
		for (int i = 0; i < events.length; i++) {
			helper = events[i];
			assertEquals("Event " + i + " unexpected result.", helper.expectedConsumeResult,
					GenericStyledAreaBehavior.isControlKeyEvent(helper.keyEvent));
		}
	}

	/**
	 * Small helper class. Allows to make tests faster.
	 */
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
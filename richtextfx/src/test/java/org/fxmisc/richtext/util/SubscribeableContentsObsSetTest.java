package org.fxmisc.richtext.util;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.junit.Test;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.value.Val;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubscribeableContentsObsSetTest {

    private class BoxedProperty implements Comparable<BoxedProperty> {

        private final int badHash;

        public BoxedProperty() {
            this(0);
        }

        public BoxedProperty(int hash) {
            badHash = hash;
        }

        final SimpleIntegerProperty property = new SimpleIntegerProperty(0);

        // create a stream of int values for property
        final EventStream<Integer> intValues = Val.wrap(property)
                .values()
                .map(Number::intValue)
                // ignore the first 0 value
                .filter(n -> n != 0);

        void addOne() { property.set(property.get() + 1);}

        @Override
        public int compareTo(BoxedProperty o) {
            return Integer.compare(badHash, o.badHash);
        }

        @Override
        public String toString() {
            return "BoxedProperty@" + hashCode();
        }

        @Override
        public int hashCode() {
            return badHash;
        }
    }

    @Test
    public void adding_subscriber_after_content_is_added_will_subscribe_to_changes() {
        SubscribeableContentsObsSet<BoxedProperty> contentSet = new SubscribeableContentsObsSet<>();

        BoxedProperty box = new BoxedProperty();
        contentSet.add(box);

        List<Integer> storageList = new ArrayList<>();

        // when property is set to a new value, store the new value in storageList
        contentSet.addSubscriber(b -> b.intValues.subscribe(storageList::add));

        int numberOfTimes = 3;
        FOR(numberOfTimes, box::addOne);
        assertEquals(numberOfTimes, storageList.size());
    }

    @Test
    public void adding_subscriber_before_content_is_added_will_subscribe_to_changes_when_item_is_added() {
        SubscribeableContentsObsSet<BoxedProperty> contentSet = new SubscribeableContentsObsSet<>();

        List<Integer> storageList = new ArrayList<>();

        // when property is set to a new value, store the new value in storageList
        contentSet.addSubscriber(b -> b.intValues.subscribe(storageList::add));

        BoxedProperty box = new BoxedProperty();
        contentSet.add(box);

        int numberOfTimes = 3;
        FOR(numberOfTimes, box::addOne);

        assertEquals(numberOfTimes, storageList.size());
    }

    @Test
    public void removing_item_from_list_will_stop_subscription() {
        SubscribeableContentsObsSet<BoxedProperty> contentSet = new SubscribeableContentsObsSet<>();

        List<Integer> storageList = new LinkedList<>();

        // when property is set to a new value, store the new value in storageList
        contentSet.addSubscriber(b -> b.intValues.subscribe(storageList::add));

        BoxedProperty box = new BoxedProperty();
        contentSet.add(box);

        int numberOfTimes = 3;
        FOR(numberOfTimes, box::addOne);

        contentSet.remove(box);

        FOR(2, box::addOne);

        assertEquals(3, storageList.size());
    }

    @Test
    public void adding_subscriber_and_removing_it_will_not_throw_exception() {
        SubscribeableContentsObsSet<Integer> set = new SubscribeableContentsObsSet<>();
        Subscription removeSubscriber = set.addSubscriber(i -> Subscription.EMPTY);
        removeSubscriber.unsubscribe();
    }

    @Test
    public void adding_subscriber_and_later_removing_it_will_unsubscribe_from_all_elements() {
        SubscribeableContentsObsSet<BoxedProperty> contentSet = new SubscribeableContentsObsSet<>();

        List<Integer> storageList = new LinkedList<>();

        // when property is set to a new value, store the new value in storageList
        Subscription removeSubscriber = contentSet.addSubscriber(b -> b.intValues.subscribe(storageList::add));

        BoxedProperty box1 = new BoxedProperty(1);
        BoxedProperty box2 = new BoxedProperty(2);
        contentSet.add(box1);
        contentSet.add(box2);

        box1.addOne();
        box2.addOne();
        assertEquals(2, storageList.size());

        storageList.clear();
        removeSubscriber.unsubscribe();

        box1.addOne();
        box2.addOne();

        assertEquals(0, storageList.size());
    }

    @Test
    public void adding_new_subscriber_when_list_has_contents_does_not_fire_change_event() {
        SubscribeableContentsObsSet<Integer> contentSet = new SubscribeableContentsObsSet<>();

        contentSet.add(1);
        contentSet.add(2);
        contentSet.add(3);

        SimpleBooleanProperty changeWasFired = new SimpleBooleanProperty(false);
        Subscription removeChangeListener = contentSet.addChangeListener(change -> changeWasFired.set(true));

        contentSet.addSubscriber(b -> Subscription.EMPTY);

        assertFalse(changeWasFired.get());

        // cleanup
        removeChangeListener.unsubscribe();
    }

    @Test
    public void adding_new_subscriber_when_list_has_contents_does_not_fire_invalidation_event() {
        SubscribeableContentsObsSet<Integer> contentSet = new SubscribeableContentsObsSet<>();

        contentSet.add(1);
        contentSet.add(2);
        contentSet.add(3);

        // when a change occurs add the additions/removals in another list
        SimpleBooleanProperty changeWasFired = new SimpleBooleanProperty(false);

        Subscription removeInvalidationListener = contentSet.addInvalidationListener(change -> changeWasFired.set(true));

        // when property is set to a new value, store the new value in storageList
        contentSet.addSubscriber(ignore -> Subscription.EMPTY);

        assertFalse(changeWasFired.get());

        // cleanup
        removeInvalidationListener.unsubscribe();
    }

    @Test
    public void adding_and_removing_element_fires_change_event() {
        SubscribeableContentsObsSet<Integer> set = new SubscribeableContentsObsSet<>();

        SimpleBooleanProperty added = new SimpleBooleanProperty(false);
        SimpleBooleanProperty removed = new SimpleBooleanProperty(false);

        set.addChangeListener(change -> {
            if (change.wasAdded()) {
                added.set(true);
            } else if (change.wasRemoved()) {
                removed.set(true);
            }
        });

        int value = 3;

        set.add(value);
        assertTrue(added.get());
        assertFalse(removed.get());

        added.set(false);

        set.remove(value);
        assertFalse(added.get());
        assertTrue(removed.get());
    }

    @Test
    public void adding_and_removing_element_fires_invalidation_event() {
        SubscribeableContentsObsSet<Integer> set = new SubscribeableContentsObsSet<>();

        BoxedProperty box = new BoxedProperty();

        set.addInvalidationListener(change -> box.addOne());

        int value = 5;

        set.add(value);
        assertEquals(1, box.property.get());

        set.remove(value);
        assertEquals(2, box.property.get());
    }

    private void FOR(int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            action.run();
        }
    }
}

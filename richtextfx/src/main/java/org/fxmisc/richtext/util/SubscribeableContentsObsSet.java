package org.fxmisc.richtext.util;

import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.reactfx.Subscription;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * An {@link javafx.collections.ObservableSet} implementation that allows one to subscribe to updates within
 * the elements themselves. For example, if one stored {@link javafx.scene.text.Text} in this set and one wanted
 * to be notified each time one of the text's {@link javafx.scene.text.Text#textProperty() textProperty()} changed,
 * one could use {@link #addSubscriber(Function)} with the function
 * <pre><code>
 * text -&gt; {
 *     EventStream&lt;String&gt; textValues = EventStreams.nonNullValuesOf(text.textProperty());
 *     return EventStreams.combine(textValues, otherTextValuesFromSomewhereElse)
 *         .subscribe(tuple2 -&gt; {
 *              String quantity = tuple2.get1();
 *              String unit = tuple2.get2();
 *              someOtherObjectOnTheScreen.setText("Will send " + quantity + " " + unit + " to the department");
 *         });
 * }
 * </code></pre>
 *
 * When the element is removed from the set, the function's returned {@link Subscription} is
 * {@link Subscription#unsubscribe() unsubscribed} to prevent any memory leaks.
 *
 * @param <E> the type of element in the set
 */
public class SubscribeableContentsObsSet<E> extends AbstractSet<E> implements ObservableSet<E> {

    private final List<Function<? super E, Subscription>> subscribers = new LinkedList<>();
    private final List<SetChangeListener<? super E>> changeListeners = new LinkedList<>();
    private final List<InvalidationListener> invalidationListeners = new LinkedList<>();

    private final Map<E, List<Subscription>> map;

    public SubscribeableContentsObsSet() {
        this(null);
    }

    public SubscribeableContentsObsSet(Comparator<? super E> comparator) {
        this.map = new TreeMap<>(comparator);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean add(E e) {
        Objects.requireNonNull(e, "Cannot add a null object to this list");
        if (map.containsKey(e)) {
            return false;
        }

        // optimize for our use cases; initial capacity may need to be changed in future versions
        List<Subscription> list = new ArrayList<>(1);
        subscribers.stream()
                .map(f -> f.apply(e))
                .forEach(list::add);
        map.put(e, list);
        invalidateSet();
        fireElementAdded(e);

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        List<Subscription> list = map.remove(o);
        if (list == null) {
            return false;
        } else {
            list.forEach(Subscription::unsubscribe);
            invalidateSet();
            fireElementRemoved((E) o);

            return true;
        }
    }

    /**
     * Subscribes to all current and future elements' internal changes in this set until either they are removed
     * or this subscriber is removed by calling {@link Subscription#unsubscribe() unsubscribe} on the function's
     * returned {@link Subscription}.
     */
    public Subscription addSubscriber(Function<? super E, Subscription> subscriber) {
        Objects.requireNonNull(subscriber);
        subscribers.add(subscriber);

        List<E> keys = new ArrayList<>(map.keySet());
        keys.forEach(key -> {
            List<Subscription> otherSubs = map.get(key);
            Subscription sub = subscriber.apply(key);
            otherSubs.add(sub);
            map.put(key, otherSubs);
        });

        return () -> removeSubscriber(subscriber);
    }

    /**
     * Helper method for adding a change listener that can be removed by calling
     * {@link Subscription#unsubscribe() unsubscribe} on the returned {@link Subscription}.
     */
    public Subscription addChangeListener(SetChangeListener<? super E> listener) {
        addListener(listener);
        return () -> removeListener(listener);
    }

    /**
     * Helper method for adding an invalidation listener that can be removed by calling
     * {@link Subscription#unsubscribe() unsubscribe} on the returned {@link Subscription}.
     */
    public Subscription addInvalidationListener(InvalidationListener listener) {
        addListener(listener);
        return () -> removeListener(listener);
    }

    @Override
    public void addListener(SetChangeListener<? super E> listener) {
        changeListeners.add(listener);
    }

    @Override
    public void removeListener(SetChangeListener<? super E> listener) {
        changeListeners.remove(listener);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        invalidationListeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    private void invalidateSet() {
        invalidationListeners.forEach(l -> l.invalidated(this));
    }

    private void removeSubscriber(Function<? super E, Subscription> subscriber) {
        // remove the subscriber while calculating its index
        // which corresponds to the same index in the key's list.
        int index = -1;
        int i = 0;
        Iterator<Function<? super E, Subscription>> iter = subscribers.iterator();
        while (iter.hasNext() && index == -1) {
            Function<? super E, Subscription> s = iter.next();
            if (s == subscriber) {
                iter.remove();
                index = i;
            } else {
                i++;
            }
        }

        final int finalIndex = index;
        List<E> keys = new ArrayList<>(map.keySet());

        // if this subscriber is being removed, we no longer need to store
        // its corresponding subscription. Thus, it can be removed and unsubscribed
        keys.forEach(key -> map.get(key).remove(finalIndex).unsubscribe());
    }

    private void fireElementAdded(E elem) {
        SetChangeListener.Change<E> change = new SetChangeListener.Change<E>(this) {
            @Override
            public boolean wasAdded() {
                return true;
            }

            @Override
            public boolean wasRemoved() {
                return false;
            }

            @Override
            public E getElementAdded() {
                return elem;
            }

            @Override
            public E getElementRemoved() {
                return null;
            }
        };
        changeListeners.forEach(l -> l.onChanged(change));
    }

    private void fireElementRemoved(E elem) {
        SetChangeListener.Change<E> change = new SetChangeListener.Change<E>(this) {
            @Override
            public boolean wasAdded() {
                return false;
            }

            @Override
            public boolean wasRemoved() {
                return true;
            }

            @Override
            public E getElementAdded() {
                return null;
            }

            @Override
            public E getElementRemoved() {
                return elem;
            }
        };
        changeListeners.forEach(l -> l.onChanged(change));
    }
}

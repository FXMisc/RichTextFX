package org.fxmisc.easybind;

import static org.junit.Assert.*;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import org.junit.Test;

public class SelectTest {

    @Test
    public void test() {
        Property<A> root = new SimpleObjectProperty<>();
        UnbindableBinding<String> selection = EasyBind.select(root)
                .select(a -> a.b)
                .selectObject(b -> b.s);
        Counter counter = new Counter();
        selection.addListener(obs -> counter.inc());

        assertNull(selection.getValue());

        A a1 = new A();
        B b1 = new B();
        b1.s.setValue("s1");
        a1.b.setValue(b1);
        root.setValue(a1);
        assertEquals("s1", selection.getValue());
        assertEquals(1, counter.get());
        counter.reset();

        b1.s.setValue("s2");
        assertEquals("s2", selection.getValue());
        assertEquals(1, counter.get());
        counter.reset();

        B b2 = new B();
        a1.b.setValue(b2);
        assertNull(selection.getValue());
        assertEquals(1, counter.get());
        counter.reset();

        // test that changing b1 no longer invalidates the selection
        b1.s.setValue("xyz");
        assertEquals(0, counter.get());

        A a2 = new A();
        root.setValue(a2);
        assertNull(selection.getValue());
        assertEquals(1, counter.get());
        counter.reset();

        // test that changing a1 no longer invalidates the selection
        a1.b.setValue(b1);
        assertEquals(0, counter.get());

        a2.b.setValue(b1);
        assertEquals("xyz", selection.getValue());
        assertEquals(1, counter.get());
        counter.reset();

        // test that no more invalidations arrive after unbind
        selection.unbind();
        b1.s.setValue("foo");
        assertEquals(0, counter.get());
        b2.s.setValue("bar");
        assertEquals(0, counter.get());
        a1.b.setValue(new B());
        assertEquals(0, counter.get());
        a2.b.setValue(new B());
        assertEquals(0, counter.get());
        root.setValue(new A());
        assertEquals(0, counter.get());

        // test that a second call to unbind does not throw
        selection.unbind();
    }

}

class Counter {
    private int count = 0;
    public void inc() {
        count += 1;
    }
    public int get() {
        return count;
    }
    public void reset() {
        count = 0;
    }
}

class A {
    public final Property<B> b = new SimpleObjectProperty<>();
}

class B {
    public final Property<String> s = new SimpleStringProperty();
}
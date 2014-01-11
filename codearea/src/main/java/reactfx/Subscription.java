package reactfx;

public interface Subscription {
    void unsubscribe();

    static Subscription multi(Subscription... subscriptions) {
        return () -> {
            for(Subscription s: subscriptions) {
                s.unsubscribe();
            }
        };
    }
}

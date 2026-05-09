package edu.cs4b.router;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a named channel that clients can subscribe to.
 *
 * THREAD SAFETY — WHY CopyOnWriteArrayList?
 *
 * Multiple threads access the subscriber list concurrently:
 *   - ClientHandler threads call addSubscriber/removeSubscriber when
 *     their client subscribes or disconnects.
 *   - During a broadcast, the sender's ClientHandler thread iterates
 *     over all subscribers to forward the message.
 *
 * A regular ArrayList is NOT thread-safe. If one thread is iterating
 * (broadcasting) while another thread modifies the list (subscribe/
 * unsubscribe), you get a ConcurrentModificationException or worse.
 *
 * CopyOnWriteArrayList solves this by making a fresh copy of the
 * internal array every time the list is modified (add/remove). This
 * means:
 *   - Reads (iterating for broadcast) are fast and lock-free — they
 *     just read the current snapshot of the array.
 *   - Writes (subscribe/unsubscribe) are slower because they copy the
 *     entire array, but that's fine because subscribing happens rarely
 *     compared to message broadcasting.
 *
 * This is a classic read-heavy/write-light pattern. If writes were
 * frequent, we'd use a different strategy (like synchronized blocks
 * or a ReadWriteLock).
 */
public class Channel {

    private final String name;

    // Each modification (add/remove) creates a new copy of the internal array.
    // Iterators see a snapshot and will never throw ConcurrentModificationException.
    private final CopyOnWriteArrayList<ClientHandler> subscribers = new CopyOnWriteArrayList<>();

    public Channel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addSubscriber(ClientHandler client) {
        // Guard against duplicate subscriptions. contains() is O(n) but
        // the subscriber list is small (classroom scale).
        if (!subscribers.contains(client)) {
            subscribers.add(client);
        }
    }

    public void removeSubscriber(ClientHandler client) {
        subscribers.remove(client);
    }

    /**
     * Returns the live list of subscribers. Safe to iterate even while
     * other threads add/remove subscribers — CopyOnWriteArrayList
     * guarantees the iterator sees a consistent snapshot.
     */
    public List<ClientHandler> getSubscribers() {
        return subscribers;
    }

    public boolean isEmpty() {
        return subscribers.isEmpty();
    }
}

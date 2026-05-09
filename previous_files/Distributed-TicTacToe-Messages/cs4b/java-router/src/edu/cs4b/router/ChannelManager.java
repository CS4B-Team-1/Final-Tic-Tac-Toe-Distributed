package edu.cs4b.router;

import edu.cs4b.protocol.Envelope;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of channels and their subscribers.
 *
 * CHANNEL NAMING CONVENTION
 *
 * Channels use a path-like naming scheme:
 *   /lobby              — a top-level channel
 *   /game/abc123        — a specific game session
 *   /dm/alice           — a direct-message channel for Alice
 *
 * WILDCARD SUBSCRIPTIONS
 *
 * Clients can subscribe to a wildcard pattern by ending the channel
 * name with "/*". For example:
 *
 *   subscribe("/game/*")
 *
 * This means: "deliver me any message published to ANY channel that
 * starts with /game/". So a message sent to /game/abc123 or /game/xyz
 * will be delivered to this subscriber.
 *
 * This is how the GameController works — it subscribes to /game/* and
 * receives messages from ALL active games without knowing their IDs
 * in advance.
 *
 * Internally, wildcard subscriptions are stored in a SEPARATE map from
 * exact subscriptions. When broadcasting, we check BOTH:
 *   1. Exact match: subscribers of "/game/abc123"
 *   2. Wildcard match: subscribers of "/game/*" (because "/game/abc123"
 *      starts with "/game/")
 *
 * THREAD SAFETY — WHY ConcurrentHashMap?
 *
 * A regular HashMap is NOT thread-safe. If two ClientHandler threads
 * simultaneously call subscribe() for different channels, they could
 * corrupt the HashMap's internal structure (lost entries, infinite
 * loops in older Java versions, etc.).
 *
 * ConcurrentHashMap is designed for exactly this scenario:
 *   - Multiple threads can read and write concurrently without locking
 *     the entire map.
 *   - It achieves this by internally dividing the map into segments,
 *     so writes to different keys don't block each other.
 *   - computeIfAbsent() is atomic — it guarantees that the channel is
 *     created exactly once even if two threads subscribe at the same
 *     instant.
 *
 * Why not just synchronize all the methods?
 *   - That would work, but it would serialize ALL channel operations
 *     behind a single lock. With ConcurrentHashMap, subscribing to
 *     "/lobby" doesn't block a broadcast on "/game/123".
 */
public class ChannelManager {

    // Exact channel subscriptions: "/game/abc123" -> Channel
    // Used when a client subscribes to a specific channel.
    private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    // Wildcard subscriptions: "/game/*" -> Channel
    // Used when a client subscribes to a pattern like "/game/*".
    // Stored separately so we can efficiently look up exact matches
    // (the common case) while still checking wildcards on broadcast.
    private final ConcurrentHashMap<String, Channel> wildcardChannels = new ConcurrentHashMap<>();

    /**
     * Subscribe a client to a channel or wildcard pattern.
     *
     * If the channel name ends with "/*", this is a wildcard subscription.
     * The client will receive messages on ANY channel that matches the
     * prefix. For example, subscribing to "/game/*" matches "/game/123",
     * "/game/abc", etc.
     *
     * If the channel name does NOT end with "/*", this is an exact
     * subscription — the client only receives messages on that specific
     * channel.
     *
     * computeIfAbsent is atomic: if two clients subscribe to "/lobby" at the
     * exact same moment, only one Channel object is created. The method
     * returns the existing channel if one was already there, or the newly
     * created one if not.
     */
    public void subscribe(String channelName, ClientHandler client) {
        if (isWildcard(channelName)) {
            wildcardChannels.computeIfAbsent(channelName, Channel::new).addSubscriber(client);
        } else {
            channels.computeIfAbsent(channelName, Channel::new).addSubscriber(client);
        }
        System.out.println("[ChannelManager] " + client.getClientId()
                + " subscribed to '" + channelName + "'");
    }

    /**
     * Unsubscribe a client from a channel or wildcard pattern.
     * Removes the channel if empty to avoid accumulating abandoned
     * channel objects in memory.
     */
    public void unsubscribe(String channelName, ClientHandler client) {
        ConcurrentHashMap<String, Channel> map = isWildcard(channelName) ? wildcardChannels : channels;
        Channel channel = map.get(channelName);
        if (channel != null) {
            channel.removeSubscriber(client);
            if (channel.isEmpty()) {
                map.remove(channelName);
            }
            System.out.println("[ChannelManager] " + client.getClientId()
                    + " unsubscribed from '" + channelName + "'");
        }
    }

    /**
     * Remove a client from all channels (both exact and wildcard).
     * Called when a client disconnects (gracefully or due to a broken
     * connection).
     *
     * Note: iterating over a ConcurrentHashMap's entrySet() while modifying
     * it (via remove()) is safe — unlike HashMap, it will NOT throw
     * ConcurrentModificationException. The iterator sees a "weakly consistent"
     * view of the map.
     */
    public void unsubscribeAll(ClientHandler client) {
        removeFromMap(channels, client);
        removeFromMap(wildcardChannels, client);
        System.out.println("[ChannelManager] " + client.getClientId()
                + " removed from all channels");
    }

    /**
     * Broadcast an envelope to all subscribers that should receive it.
     *
     * This checks TWO things:
     *   1. Exact match — subscribers of the specific channel (e.g., "/game/abc123")
     *   2. Wildcard match — subscribers of any wildcard pattern that matches
     *      (e.g., "/game/*" matches "/game/abc123")
     *
     * A subscriber who matches via BOTH exact and wildcard only receives
     * the message once (we use a Set to deduplicate).
     *
     * The sender is excluded so clients don't receive their own messages —
     * they already know what they sent.
     *
     * Example:
     *   - Alice subscribes to "/game/abc123" (exact)
     *   - GameController subscribes to "/game/*" (wildcard)
     *   - Bob sends a MoveMessage to "/game/abc123"
     *   - Both Alice AND GameController receive it
     *   - Bob does NOT receive it (he's the sender)
     */
    public void broadcast(String channelName, Envelope envelope, ClientHandler sender) {
        // Collect all recipients into a Set to avoid sending duplicates
        // if someone is subscribed via both exact and wildcard.
        Set<ClientHandler> recipients = new HashSet<>();

        // 1. Exact match: subscribers of this specific channel
        Channel exactChannel = channels.get(channelName);
        if (exactChannel != null) {
            recipients.addAll(exactChannel.getSubscribers());
        }

        // 2. Wildcard match: check every wildcard pattern to see if it
        //    matches this channel name.
        //
        //    For example, if channelName is "/game/abc123":
        //      "/game/*"   -> prefix is "/game/"   -> matches!
        //      "/dm/*"     -> prefix is "/dm/"     -> does NOT match
        //
        //    We iterate ALL wildcard patterns. This is fine at classroom
        //    scale (a handful of patterns). A production system might use
        //    a trie for O(log n) prefix matching.
        for (var entry : wildcardChannels.entrySet()) {
            String pattern = entry.getKey();
            if (matchesWildcard(pattern, channelName)) {
                recipients.addAll(entry.getValue().getSubscribers());
            }
        }

        // Deliver to all recipients except the sender
        for (ClientHandler recipient : recipients) {
            if (recipient != sender) {
                recipient.sendEnvelope(envelope);
            }
        }
    }

    // --- Private helpers ---

    /**
     * Check if a subscription string is a wildcard pattern.
     * Wildcard patterns end with "/*".
     */
    private boolean isWildcard(String channelName) {
        return channelName.endsWith("/*");
    }

    /**
     * Check if a wildcard pattern matches a specific channel name.
     *
     * The pattern "/game/*" matches any channel that starts with "/game/".
     * We strip the trailing "*" to get the prefix, then check if the
     * channel name starts with that prefix.
     *
     * Examples:
     *   matchesWildcard("/game/*", "/game/abc123")  -> true
     *   matchesWildcard("/game/*", "/game/xyz")     -> true
     *   matchesWildcard("/game/*", "/dm/alice")     -> false
     *   matchesWildcard("/game/*", "/game/")        -> true  (edge case, but fine)
     */
    private boolean matchesWildcard(String pattern, String channelName) {
        // "/game/*" -> prefix is "/game/"
        String prefix = pattern.substring(0, pattern.length() - 1);
        return channelName.startsWith(prefix);
    }

    /**
     * Remove a client from every channel in the given map.
     * Cleans up empty channels.
     */
    private void removeFromMap(ConcurrentHashMap<String, Channel> map, ClientHandler client) {
        for (var entry : map.entrySet()) {
            entry.getValue().removeSubscriber(client);
            if (entry.getValue().isEmpty()) {
                map.remove(entry.getKey());
            }
        }
    }
}

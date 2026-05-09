# CS4B — Distributed Tic-Tac-Toe Message-Passing Framework

A teaching framework that provides the networking, multithreading, and serialization infrastructure for building a distributed tic-tac-toe game using **message passing** over TCP sockets.

Students focus on designing the **messages** and **game logic**. The framework handles everything else: connections, channels, subscriptions, serialization, and thread safety.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [How It Works](#how-it-works)
3. [Channel Naming and Wildcard Subscriptions](#channel-naming-and-wildcard-subscriptions)
4. [Project Structure](#project-structure)
5. [Prerequisites](#prerequisites)
6. [Building and Running](#building-and-running)
7. [Example Session](#example-session)
8. [Key Concepts for Students](#key-concepts-for-students)
   - [The Protocol Layer](#the-protocol-layer)
   - [The Wire Format](#the-wire-format)
   - [Threading Model](#threading-model)
   - [Thread Safety](#thread-safety)
   - [Serialization](#serialization)
9. [Creating Your Own Messages](#creating-your-own-messages)
10. [Using the Client Library](#using-the-client-library)
11. [Class Reference](#class-reference)

---

## Architecture Overview

```
                         ┌─────────────────────────────────────────┐
                         │             ROUTER (Server)              │
                         │            "Dumb Relay"                  │
                         │                                         │
 ┌─────────────┐   TCP   │  ┌───────────────────────────────────┐  │   TCP   ┌─────────────┐
 │   Client A  ├────────►│  │        ChannelManager             │  │◄────────┤   Client B  │
 │ (e.g. Alice)│◄────────┤  │                                   │  ├────────►│ (e.g. Bob)  │
 └─────────────┘         │  │ Exact channels:                   │  │         └─────────────┘
                         │  │   /lobby        ──► [A, B]        │  │
                         │  │   /game/abc123  ──► [A]           │  │
 ┌─────────────┐   TCP   │  │   /game/xyz789  ──► [B]           │  │
 │   Client C  ├────────►│  │                                   │  │
 │  (GameCtrl) │◄────────┤  │ Wildcard channels:                │  │
 └─────────────┘         │  │   /game/*       ──► [C]           │  │
                         │  └───────────────────────────────────┘  │
                         └─────────────────────────────────────────┘
```

The system has three layers:

1. **Router (Server)** — A "dumb relay" that accepts TCP connections and routes messages between clients. It knows about **channels** and **subscribers**, but never looks at message content. It does not understand game rules, turns, or boards.

2. **Client Library (`RouterClient`)** — Handles the networking so your application code doesn't have to. Provides simple methods: `connect()`, `subscribe()`, `send()`, `disconnect()`.

3. **Your Application** — Player clients, game controllers, or anything else. You define your own `Message` types and the logic for when to send/receive them.

---

## How It Works

Communication happens through **channels**. Think of them like chat rooms:

1. A client **connects** to the router over TCP.
2. The client **subscribes** to one or more named channels (e.g., `"/lobby"`, `"/game/abc123"`).
3. When a client **sends** a message to a channel, the router delivers it to **every other subscriber** on that channel (the sender does NOT receive their own message).
4. Clients can **unsubscribe** from channels at any time.
5. When a client **disconnects**, the router automatically removes it from all channels.

Every message on the wire is wrapped in an **Envelope** — a container that carries the message type, channel name, sender ID, and the actual message payload.

```
┌──────────────────────────────────────────────────┐
│                    Envelope                       │
│                                                  │
│  type:     MESSAGE                               │
│  channel:  "/game/abc123"                        │
│  senderId: "client-1"    (stamped by router)     │
│  payload:  [serialized MoveMessage bytes]        │
└──────────────────────────────────────────────────┘
```

---

## Channel Naming and Wildcard Subscriptions

### Channel Naming Convention

Channels use a **path-like naming scheme**, similar to URL paths or file paths:

```
/lobby              — global chat / matchmaking
/game/abc123        — a specific game session
/game/xyz789        — another game session
/dm/alice           — direct messages for Alice
```

This naming convention is important because it enables **wildcard subscriptions**.

### Wildcard Subscriptions

A client can subscribe to a **wildcard pattern** by ending the channel name with `/*`. This means: "deliver me any message published to any channel that starts with this prefix."

```java
// The GameController subscribes to ALL game channels at once:
client.subscribe("/game/*", listener);

// Now it receives messages sent to ANY of these:
//   /game/abc123
//   /game/xyz789
//   /game/anything
```

This is how the **GameController** works:

```
┌─────────┐  sends MoveMessage   ┌────────┐  delivers to /game/abc123   ┌─────────┐
│  Alice  ├─── to /game/abc123 ──►│ Router ├────────────────────────────►│  Bob    │
│         │                       │        │  also delivers to /game/*   │(exact)  │
└─────────┘                       │        ├────────────────────────────►┌─────────┐
                                  └────────┘                            │GameCtrl │
                                                                        │(wildcard│
┌─────────┐  sends MoveMessage   ┌────────┐  delivers to /game/xyz789   └─────────┘
│ Charlie ├─── to /game/xyz789 ──►│ Router ├────────────────────────────►┌─────────┐
│         │                       │        │  also delivers to /game/*   │  Dave   │
└─────────┘                       │        ├────────────────────────────►│(exact)  │
                                  └────────┘                            ┌─────────┐
                                                                        │GameCtrl │
                                                                        │(wildcard│
                                                                        └─────────┘
```

The GameController doesn't need to know game IDs in advance — it subscribes once to `/game/*` and automatically receives messages from every game.

### How Wildcard Matching Works

The matching rule is simple: strip the `*`, and check if the channel name starts with the remaining prefix.

| Pattern | Channel | Match? | Why |
|---------|---------|--------|-----|
| `/game/*` | `/game/abc123` | Yes | starts with `/game/` |
| `/game/*` | `/game/xyz789` | Yes | starts with `/game/` |
| `/game/*` | `/lobby` | No | does not start with `/game/` |
| `/game/*` | `/game-old/1` | No | does not start with `/game/` |
| `/dm/*` | `/dm/alice` | Yes | starts with `/dm/` |
| `/dm/*` | `/game/abc123` | No | does not start with `/dm/` |

### Exact vs. Wildcard — What Gets Stored Where

The router keeps two separate maps:

```
Exact channels (ConcurrentHashMap):
  "/lobby"         → [Alice, Bob]
  "/game/abc123"   → [Alice, Bob]
  "/game/xyz789"   → [Charlie, Dave]

Wildcard channels (ConcurrentHashMap):
  "/game/*"        → [GameController]
  "/dm/*"          → [ChatService]
```

When a message is sent to `/game/abc123`, the router:
1. Looks up exact subscribers of `/game/abc123` → Alice, Bob
2. Checks every wildcard pattern — `/game/*` matches → GameController
3. Delivers to all three (minus the sender)

---

## Project Structure

There are two independent Java projects. Protocol files are intentionally duplicated across both so you never need a shared library or build tool dependency.

```
cs4b/
├── README.md
│
├── java-router/                          # The server
│   └── src/edu/cs4b/
│       ├── protocol/                     # Shared protocol (also in client)
│       │   ├── Message.java              # Marker interface — YOU implement this
│       │   ├── TextMessage.java          # Built-in simple text message
│       │   ├── Envelope.java             # Wire-level wrapper
│       │   └── EnvelopeType.java         # SUBSCRIBE, UNSUBSCRIBE, MESSAGE, ACK, ERROR
│       └── router/                       # Server implementation
│           ├── RouterMain.java           # Entry point — starts the server
│           ├── ClientHandler.java        # One thread per connected client
│           ├── ChannelManager.java       # Thread-safe channel registry + wildcard matching
│           └── Channel.java             # A channel and its subscriber list
│
└── java-player-client/                   # Example client application
    └── src/edu/cs4b/
        ├── protocol/                     # Same 4 protocol files (duplicated)
        │   ├── Message.java
        │   ├── TextMessage.java
        │   ├── Envelope.java
        │   └── EnvelopeType.java
        ├── client/                       # Reusable client library
        │   ├── RouterClient.java         # Connect, subscribe, send, receive
        │   └── MessageListener.java      # Callback interface for incoming messages
        └── player/                       # Example application code
            ├── PlayerMain.java           # Interactive demo — run this!
            ├── JoinMessage.java          # Example Message: carries playerName
            ├── MoveMessage.java          # Example Message: carries row, col
            └── EmojiMessage.java         # Example Message: carries emoji, repeatCount
```

---

## Prerequisites

- **Java 17 or later**

Verify your installation:

```bash
java -version
javac -version
```

---

## Building and Running

### Step 1: Compile Both Projects

All commands should be run from the `cs4b/` root directory (the parent that contains `java-router/` and `java-player-client/`).

```bash
cd cs4b

# Compile the router
javac -d java-router/out java-router/src/edu/cs4b/protocol/*.java java-router/src/edu/cs4b/router/*.java

# Compile the player client
javac -d java-player-client/out java-player-client/src/edu/cs4b/protocol/*.java java-player-client/src/edu/cs4b/client/*.java java-player-client/src/edu/cs4b/player/*.java
```

The `-d <dir>/out` flag puts compiled `.class` files into an `out/` directory inside each project, keeping things tidy.

### Step 2: Start the Router

Open a terminal in the `cs4b/` directory and run:

```bash
java -cp java-router/out edu.cs4b.router.RouterMain
```

You should see:

```
[Router] Listening on port 4000
```

The router is now waiting for client connections. To use a different port:

```bash
java -cp java-router/out edu.cs4b.router.RouterMain 8080
```

### Step 3: Start Player Clients

Open **two more terminals** in the `cs4b/` directory (one for each player):

**Terminal 2 — Alice:**

```bash
java -cp java-player-client/out edu.cs4b.player.PlayerMain --name Alice
```

**Terminal 3 — Bob:**

```bash
java -cp java-player-client/out edu.cs4b.player.PlayerMain --name Bob
```

Each player connects to the router, subscribes to `/lobby`, and enters an interactive command loop.

Full command-line options:

```
java -cp java-player-client/out edu.cs4b.player.PlayerMain [options]

Options:
  --name <name>    Your player name (default: random like "Player-a1b2")
  --host <host>    Router hostname (default: localhost)
  --port <port>    Router port (default: 4000)
```

---

## Example Session

### Basic Demo: Two Players Chatting

Below is what you'll see across three terminals. Lines starting with `>` are what you type.

**Terminal 1 — Router:**

```
[Router] Listening on port 4000
[Router] client-1 connected from /127.0.0.1:52007
[ChannelManager] client-1 subscribed to '/lobby'
[Router] client-2 connected from /127.0.0.1:52008
[ChannelManager] client-2 subscribed to '/lobby'
```

**Terminal 2 — Alice (client-1):**

```
Connected to router at localhost:4000 as client-1

Commands:
  say <text>           Send a chat message to /lobby
  move <row> <col>     Send a move to your game channel
  emoji <emoji> <n>    Send an emoji n times to /lobby
  join <gameId>        Join game channel /game/<gameId>
  leave <gameId>       Leave game channel /game/<gameId>
  quit                 Disconnect and exit

[/lobby] client-2 joined: Bob           ← Alice sees Bob's JoinMessage
> say hello Bob!                         ← Alice sends a TextMessage
> emoji 🎉 3                             ← Alice sends an EmojiMessage
> quit
Disconnected from router
```

**Terminal 3 — Bob (client-2):**

```
Connected to router at localhost:4000 as client-2

Commands:
  say <text>           Send a chat message to /lobby
  move <row> <col>     Send a move to your game channel
  emoji <emoji> <n>    Send an emoji n times to /lobby
  join <gameId>        Join game channel /game/<gameId>
  leave <gameId>       Leave game channel /game/<gameId>
  quit                 Disconnect and exit

[/lobby] client-1 says: hello Bob!       ← Bob receives Alice's TextMessage
[/lobby] client-1 sent: 🎉🎉🎉            ← Bob receives Alice's EmojiMessage
```

Notice how different message types (`JoinMessage`, `TextMessage`, `EmojiMessage`) all flow through the same `/lobby` channel. The listener uses `instanceof` to tell them apart — this is **polymorphism** in action.

### Game Channels: Joining a Game

Players can join game-specific channels using the `join` command:

**Terminal 2 — Alice:**

```
> join abc123                            ← subscribes to /game/abc123
Joined /game/abc123
> move 0 1                               ← sends MoveMessage to /game/abc123
[/game/abc123] client-2 played: (1, 1)   ← Bob's move on the same game
```

**Terminal 3 — Bob:**

```
> join abc123                            ← subscribes to /game/abc123
Joined /game/abc123
[/game/abc123] client-1 played: (0, 1)   ← Alice's move
> move 1 1                               ← Bob's move
```

### Wildcard Demo: Simulating a GameController

To see wildcard subscriptions in action, you can write a simple GameController that watches all game channels:

```java
RouterClient controller = new RouterClient("localhost", 4000);
controller.connect();

// Subscribe to ALL game channels with one wildcard
controller.subscribe("/game/*", (channel, senderId, message) -> {
    System.out.println("[" + channel + "] " + senderId + ": " + message);
});

// Now ANY message sent to /game/abc123, /game/xyz789, etc.
// will be delivered to this listener.
```

With the router running and the controller subscribed to `/game/*`:

```
[/game/abc123] client-2: MoveMessage{row=0, col=1}     ← Alice's move in game abc123
[/game/abc123] client-3: MoveMessage{row=1, col=1}     ← Bob's move in game abc123
[/game/xyz789] client-4: MoveMessage{row=2, col=0}     ← Charlie's move in game xyz789
```

The controller receives messages from **every** game channel without knowing the game IDs in advance.

---

## Key Concepts for Students

### The Protocol Layer

Every message you send must implement the `Message` interface:

```java
public interface Message extends Serializable {
    // Marker interface — no methods to implement.
    // Just add your own fields, constructor, and getters.
}
```

Messages are wrapped in an `Envelope` before being sent over the network:

```java
public class Envelope implements Serializable {
    EnvelopeType type;      // What kind of envelope (SUBSCRIBE, MESSAGE, etc.)
    String channel;         // Which channel this is for
    String senderId;        // Who sent it (stamped by the router)
    byte[] payloadBytes;    // Your Message, serialized to bytes
}
```

The `EnvelopeType` enum tells the router what to do:

| Type | Direction | Purpose |
|------|-----------|---------|
| `SUBSCRIBE` | Client → Router | "I want to receive messages on this channel" |
| `UNSUBSCRIBE` | Client → Router | "Stop sending me messages on this channel" |
| `MESSAGE` | Client → Router → Clients | Carries your actual game message |
| `ACK` | Router → Client | "Your subscribe/unsubscribe was successful" |
| `ERROR` | Router → Client | "Something went wrong" |

### The Wire Format

All data flows over the TCP socket as serialized Java objects using `ObjectOutputStream` and `ObjectInputStream`:

```
Client                                        Router
  │                                              │
  │──── ObjectOutputStream.writeObject(env) ────►│
  │                                              │
  │◄─── ObjectOutputStream.writeObject(env) ─────│
  │                                              │
```

Both sides create `ObjectOutputStream` **before** `ObjectInputStream`. This is critical — see [Serialization](#serialization) below for why.

### Threading Model

```
ROUTER PROCESS                               CLIENT PROCESS
┌──────────────────────┐                     ┌──────────────────────┐
│                      │                     │                      │
│  Main Thread         │                     │  Main Thread         │
│  ┌────────────────┐  │                     │  ┌────────────────┐  │
│  │ Accept Loop:   │  │                     │  │ Your code:     │  │
│  │ wait for new   │  │                     │  │ connect()      │  │
│  │ connections    │  │                     │  │ subscribe()    │  │
│  └────────────────┘  │                     │  │ send()         │  │
│         │            │                     │  │ disconnect()   │  │
│    (spawns)          │                     │  └────────────────┘  │
│         ▼            │                     │                      │
│  ClientHandler       │                     │  Listener Thread     │
│  Threads (1 per      │                     │  ┌────────────────┐  │
│  client)             │                     │  │ Blocks on      │  │
│  ┌────────────────┐  │                     │  │ readObject()   │  │
│  │ Read loop:     │  │                     │  │ Invokes your   │  │
│  │ wait for data  │  │                     │  │ MessageListener│  │
│  │ from THIS      │  │                     │  │ callbacks      │  │
│  │ client         │  │                     │  └────────────────┘  │
│  └────────────────┘  │                     │                      │
│                      │                     │                      │
│  ┌────────────────┐  │                     └──────────────────────┘
│  │ Another        │  │
│  │ ClientHandler  │  │
│  └────────────────┘  │
│        ...           │
└──────────────────────┘
```

**Router side:**
- The **main thread** runs a `ServerSocket.accept()` loop. Each time a client connects, it spawns a new `ClientHandler` thread dedicated to that client.
- Each **ClientHandler thread** blocks on `in.readObject()`, waiting for its client to send something. When it receives a `MESSAGE`, it's this thread (the sender's thread) that iterates over subscribers and delivers the message to each one.

**Client side:**
- The **main thread** is where your application code runs — calling `connect()`, `subscribe()`, `send()`, etc.
- A background **listener thread** (daemon) blocks on `in.readObject()`, waiting for the router to send something. When a message arrives, it invokes your `MessageListener` callbacks.

### Thread Safety

Multiple threads access shared data structures concurrently. Here's how we keep things safe:

#### `CopyOnWriteArrayList` (in `Channel.java`)

The subscriber list is read (during broadcasts) far more often than it's written (subscribe/unsubscribe). `CopyOnWriteArrayList` is optimized for this:

- **Every write** (add/remove) creates a **fresh copy** of the internal array. This is slow, but subscribe/unsubscribe is rare.
- **Every read** (iteration) uses the current array snapshot with **no locking**. This is fast, and an iterator will never throw `ConcurrentModificationException`.

A regular `ArrayList` would break: if one thread is iterating (broadcasting) while another modifies the list (subscribing), you get `ConcurrentModificationException` or data corruption.

#### `ConcurrentHashMap` (in `ChannelManager.java`)

The channel registry maps channel names to `Channel` objects. `ConcurrentHashMap` allows multiple threads to read and write concurrently without locking the entire map:

- `computeIfAbsent()` atomically checks if a channel exists and creates it if not — even if two clients subscribe to the same channel at the exact same instant.
- Unlike `HashMap`, iterating while another thread modifies entries is safe (weakly consistent).

The `ChannelManager` uses **two** `ConcurrentHashMap` instances: one for exact channel subscriptions and one for wildcard patterns. This keeps exact lookups fast (direct hash lookup) while wildcard matching iterates the smaller wildcard map.

#### `synchronized` (in `ClientHandler.sendEnvelope()`)

`ObjectOutputStream` is **not** thread-safe. If two threads call `writeObject()` simultaneously, their bytes interleave and corrupt the stream. The `synchronized` keyword on `sendEnvelope()` ensures only one thread writes at a time.

When does this happen? When two clients send messages to the same channel simultaneously, their ClientHandler threads both try to forward to the same subscriber — two threads writing to one `ObjectOutputStream`.

#### `AtomicInteger` (in `RouterMain.java`)

Generates unique client IDs. A regular `int++` is a read-modify-write operation that's **not atomic** — two threads could read the same value and both increment to the same number. `AtomicInteger.incrementAndGet()` is guaranteed to return a unique value even under concurrent access.

#### `volatile` (in `RouterClient.java`)

The `running` flag is set by the main thread (on `disconnect()`) and read by the listener thread (in the read loop). Without `volatile`, the listener thread might cache an old value and never see the update. `volatile` ensures changes are immediately visible to all threads.

### Serialization

Java's built-in serialization converts objects to bytes and back:

```
Object  ──ObjectOutputStream.writeObject()──►  bytes on the wire
bytes   ──ObjectInputStream.readObject()────►  Object
```

For this to work, every class you send must implement `java.io.Serializable`.

**The OOS-before-OIS rule:**

When you create an `ObjectOutputStream`, it writes a **stream header** to the underlying stream. When you create an `ObjectInputStream`, it reads that header. If both sides try to create `ObjectInputStream` first, they both block waiting for the other's header — **deadlock**.

The convention: always create `ObjectOutputStream` first, `flush()` it, then create `ObjectInputStream`.

```java
// CORRECT — do this on BOTH sides:
out = new ObjectOutputStream(socket.getOutputStream());
out.flush();   // send the header
in = new ObjectInputStream(socket.getInputStream());  // reads the other side's header

// WRONG — deadlock:
in = new ObjectInputStream(socket.getInputStream());   // blocks waiting for header
out = new ObjectOutputStream(socket.getOutputStream()); // never reached
```

**Why `byte[]` payloads?**

The `Envelope` stores the message payload as `byte[]` instead of a `Message` object. This is important:

- `ObjectInputStream.readObject()` deserializes the **entire object graph** — including any nested objects.
- If the `Envelope` contained a `MoveMessage` directly, the router would need the `MoveMessage` class on its classpath, or it throws `ClassNotFoundException`.
- By pre-serializing the `Message` to `byte[]` on the client side, the router sees just a byte array. It forwards the bytes without knowing (or caring) what class they represent.
- The receiving client deserializes the bytes back into a `Message`. Since clients have the message classes, this works perfectly.

This keeps the router truly "dumb" — it never needs student code.

**`serialVersionUID`:**

Each `Serializable` class should declare a `serialVersionUID`:

```java
private static final long serialVersionUID = 1L;
```

This is a version number for the class's serialized form. If you change a class's fields and forget to update this, Java may reject deserialized objects from an older version. For classwork, just set it to `1L` and don't worry about it.

---

## Creating Your Own Messages

To create a new message type, write a class that implements `Message` and add whatever fields you need:

```java
package edu.cs4b.player;

import edu.cs4b.protocol.Message;

public class GameStartMessage implements Message {
    private static final long serialVersionUID = 1L;

    private final String player1;
    private final String player2;
    private final String startingSymbol;  // "X" or "O"

    public GameStartMessage(String player1, String player2, String startingSymbol) {
        this.player1 = player1;
        this.player2 = player2;
        this.startingSymbol = startingSymbol;
    }

    public String getPlayer1() { return player1; }
    public String getPlayer2() { return player2; }
    public String getStartingSymbol() { return startingSymbol; }
}
```

That's it. No registration, no configuration. Send it:

```java
client.send("/game/abc123", new GameStartMessage("Alice", "Bob", "X"));
```

Receive it:

```java
client.addMessageListener("/game/abc123", (channel, senderId, message) -> {
    if (message instanceof GameStartMessage start) {
        System.out.println("Game started: " + start.getPlayer1()
            + " vs " + start.getPlayer2());
    }
});
```

### Polymorphism in Action

Multiple message types can flow through the same channel. The listener uses `instanceof` with pattern matching (Java 17+) to handle each type:

```java
client.addMessageListener("/lobby", (channel, senderId, message) -> {
    if (message instanceof JoinMessage join) {
        System.out.println(senderId + " joined as " + join.getPlayerName());

    } else if (message instanceof MoveMessage move) {
        System.out.println(senderId + " played (" + move.getRow() + "," + move.getCol() + ")");

    } else if (message instanceof EmojiMessage emoji) {
        System.out.println(senderId + " sent " + emoji.render());

    } else if (message instanceof TextMessage text) {
        System.out.println(senderId + ": " + text.getText());

    } else {
        System.out.println(senderId + " sent unknown: " + message);
    }
});
```

The framework doesn't care what `Message` classes you create — it serializes them to bytes, sends them, and deserializes them on the other end. You decide what messages to define and what they mean.

### Wildcard Listeners

If you subscribe to a wildcard pattern, register your listener under the wildcard key. The framework will match incoming messages from any matching channel to your listener:

```java
// GameController: listen to ALL game channels
client.subscribe("/game/*", (channel, senderId, message) -> {
    // 'channel' tells you WHICH specific game this is from
    System.out.println("[" + channel + "] " + senderId + ": " + message);

    if (message instanceof MoveMessage move) {
        // Process the move for the game identified by 'channel'
        processMove(channel, senderId, move);
    }
});
```

---

## Using the Client Library

The `RouterClient` class is your interface to the router. Here's the full lifecycle:

```java
import edu.cs4b.client.RouterClient;
import edu.cs4b.protocol.TextMessage;

public class MyApp {
    public static void main(String[] args) throws Exception {

        // 1. Create a client
        RouterClient client = new RouterClient("localhost", 4000);

        // 2. Connect to the router (blocks until connected)
        client.connect();
        System.out.println("My ID: " + client.getClientId());

        // 3. Register a listener BEFORE subscribing (so you don't miss messages)
        client.addMessageListener("/lobby", (channel, senderId, message) -> {
            System.out.println("[" + channel + "] " + senderId + ": " + message);
        });

        // 4. Subscribe to a channel
        client.subscribe("/lobby");

        // 5. Send messages
        client.send("/lobby", new TextMessage("Hello everyone!"));

        // 6. Unsubscribe when done with a channel
        client.unsubscribe("/lobby");

        // 7. Disconnect when done entirely
        client.disconnect();
    }
}
```

### Convenience: Subscribe + Listen in One Call

```java
client.subscribe("/lobby", (channel, senderId, message) -> {
    System.out.println(senderId + ": " + message);
});
```

### Multiple Channels

A client can subscribe to many channels simultaneously:

```java
client.subscribe("/lobby");            // global chat
client.subscribe("/game/abc123");      // a specific game
client.subscribe("/game/*");           // ALL games (wildcard)
```

### Checking Connection State

```java
if (client.isConnected()) {
    client.send("/lobby", new TextMessage("still here!"));
}
```

---

## Class Reference

### Protocol Layer (shared)

| Class | Type | Purpose |
|-------|------|---------|
| `Message` | Interface | Marker interface for all application messages. Extends `Serializable`. |
| `TextMessage` | Class | Built-in message carrying a `String text` field. |
| `Envelope` | Class | Wire-level wrapper: `type`, `channel`, `senderId`, `payloadBytes`. |
| `EnvelopeType` | Enum | `SUBSCRIBE`, `UNSUBSCRIBE`, `MESSAGE`, `ACK`, `ERROR` |

### Router

| Class | Purpose |
|-------|---------|
| `RouterMain` | Entry point. `ServerSocket` accept loop, spawns `ClientHandler` threads. |
| `ClientHandler` | One per client. Reads envelopes, dispatches by type, forwards messages. |
| `ChannelManager` | Thread-safe channel registry. Handles subscribe, unsubscribe, broadcast. Supports exact and wildcard (`/*`) channel matching. |
| `Channel` | A named channel with a `CopyOnWriteArrayList` of subscribers. |

### Client Library

| Class | Purpose |
|-------|---------|
| `RouterClient` | Connect to router, subscribe to channels (exact or wildcard), send/receive messages. |
| `MessageListener` | `@FunctionalInterface` — callback for received messages: `onMessage(channel, senderId, message)`. |

### Example Messages

| Class | Fields | Purpose |
|-------|--------|---------|
| `JoinMessage` | `String playerName` | Announce a player joining. |
| `MoveMessage` | `int row, int col` | A game move on the board. |
| `EmojiMessage` | `String emoji, int repeatCount` | Send an emoji N times. Has `render()` method. |

---

## Quick Reference

**All commands below assume you are in the `cs4b/` root directory** (the parent directory that contains `java-router/` and `java-player-client/`).

```bash
# First, cd into the root project directory
cd cs4b

# Compile both projects
javac -d java-router/out java-router/src/edu/cs4b/protocol/*.java java-router/src/edu/cs4b/router/*.java
javac -d java-player-client/out java-player-client/src/edu/cs4b/protocol/*.java java-player-client/src/edu/cs4b/client/*.java java-player-client/src/edu/cs4b/player/*.java
javac -d java-game-controller/out java-game-controller/src/edu/cs4b/protocol/*.java java-game-controller/src/edu/cs4b/client/*.java java-game-controller/src/edu/cs4b/gamecontroller/*.java

# Run (3 separate terminals, each in the cs4b/ directory)
java -cp java-router/out edu.cs4b.router.RouterMain

java -cp java-game-controller/out edu.cs4b.gamecontroller.GameControllerMain
java -cp java-player-client/out edu.cs4b.player.PlayerMain --name Alice
java -cp java-player-client/out edu.cs4b.player.PlayerMain --name Bob

# Stop the router (same on macOS, Linux, and Windows)
Ctrl+C

# Commands in player client
say <text>           # Send a chat message to /lobby
move <row> <col>     # Send a move to your current game channel
emoji <emoji> <n>    # Send an emoji n times to /lobby
join <gameId>        # Subscribe to /game/<gameId>
leave <gameId>       # Unsubscribe from /game/<gameId>
quit                 # Disconnect and exit
```

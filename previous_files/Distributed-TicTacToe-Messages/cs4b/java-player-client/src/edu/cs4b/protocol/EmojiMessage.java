package edu.cs4b.protocol;

/**
 * Sends an emoji repeated a number of times.
 * Demonstrates a Message with mixed field types (String + int).
 */
public class EmojiMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final String emoji;
    private final int repeatCount;

    public EmojiMessage(String emoji, int repeatCount) {
        this.emoji = emoji;
        this.repeatCount = repeatCount;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    /** Returns the emoji repeated the specified number of times. */
    public String render() {
        return emoji.repeat(repeatCount);
    }

    @Override
    public String toString() {
        return "EmojiMessage{emoji='" + emoji + "', repeatCount=" + repeatCount + "}";
    }
}

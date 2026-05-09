package edu.cs4b.protocol;

/**
 * A simple text message. Useful for chat, error descriptions,
 * and testing the framework.
 */
public class TextMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final String text;

    public TextMessage(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "TextMessage{'" + text + "'}";
    }
}

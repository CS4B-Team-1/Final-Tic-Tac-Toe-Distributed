package edu.cs4b.protocol;

/**
 * The types of envelopes that can be sent between clients and the router.
 */
public enum EnvelopeType {
    /** Client -> Router: subscribe to a channel */
    SUBSCRIBE,

    /** Client -> Router: unsubscribe from a channel */
    UNSUBSCRIBE,

    /** Bidirectional: carries a Message payload on a channel */
    MESSAGE,

    /** Router -> Client: acknowledges a successful operation */
    ACK,

    /** Router -> Client: reports an error */
    ERROR
}

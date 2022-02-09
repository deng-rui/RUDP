package net.udp;


/**
 * This class specifies the RUDP parameters of a socket.
 *
 * @author Adrian Granados
 * @see    net.udp.ReliableSocket
 */
public class ReliableSocketProfile {
    public final static int MAX_SEND_QUEUE_SIZE    = 32;
    public final static int MAX_RECV_QUEUE_SIZE    = 32;

    public final static int MAX_SEGMENT_SIZE       = 128;
    public final static int MAX_OUTSTANDING_SEGS   = 3;
    public final static int MAX_RETRANS            = 3;
    public final static int MAX_CUMULATIVE_ACKS    = 3;
    public final static int MAX_OUT_OF_SEQUENCE    = 3;
    public final static int MAX_AUTO_RESET         = 3;
    public final static int NULL_SEGMENT_TIMEOUT   = 2000;
    public final static int RETRANSMISSION_TIMEOUT = 600;
    public final static int CUMULATIVE_ACK_TIMEOUT = 300;

    /**
     * Creates a profile with the default RUDP parameter values.
     *
     * Note: According to the RUDP protocol's draft, the default
     * maximum number of retransmissions is 3. However, if packet
     * drops are too high, the connection may get stall unless
     * the sender continues to retransmit packets that have not been
     * unacknowledged. We will use 0 instead, which means unlimited.
     *
     */
    public ReliableSocketProfile() {
        this(MAX_SEND_QUEUE_SIZE,
             MAX_RECV_QUEUE_SIZE,
             MAX_SEGMENT_SIZE,
             MAX_OUTSTANDING_SEGS,
             /* MAX_RETRANS */
             0,
             MAX_CUMULATIVE_ACKS,
             MAX_OUT_OF_SEQUENCE,
             MAX_AUTO_RESET,
             NULL_SEGMENT_TIMEOUT,
             RETRANSMISSION_TIMEOUT,
             CUMULATIVE_ACK_TIMEOUT);
    }

    /**
     * Creates an profile with the specified RUDP parameter values.
     *
     * @param maxSendQueueSize      maximum send queue size (packets).
     * @param maxRecvQueueSize      maximum receive queue size (packets).
     * @param maxSegmentSize        maximum segment size (octets) (must be at least 22).
     * @param maxOutstandingSegs    maximum number of outstanding segments.
     * @param maxRetrans            maximum number of consecutive retransmissions (0 means unlimited).
     * @param maxCumulativeAcks     maximum number of unacknowledged received segments.
     * @param maxOutOfSequence      maximum number of out-of-sequence received segments.
     * @param maxAutoReset          maximum number of consecutive auto resets (not used).
     * @param nullSegmentTimeout    null segment timeout (ms).
     * @param retransmissionTimeout retransmission timeout (ms).
     * @param cumulativeAckTimeout  cumulative acknowledge timeout (ms).
     */
    public ReliableSocketProfile(int maxSendQueueSize,
                                 int maxRecvQueueSize,
                                 int maxSegmentSize,
                                 int maxOutstandingSegs,
                                 int maxRetrans,
                                 int maxCumulativeAcks,
                                 int maxOutOfSequence,
                                 int maxAutoReset,
                                 int nullSegmentTimeout,
                                 int retransmissionTimeout,
                                 int cumulativeAckTimeout) {
        checkValue("maxSendQueueSize",      maxSendQueueSize,      1,   255);
        checkValue("maxRecvQueueSize",      maxRecvQueueSize,      1,   255);
        checkValue("maxSegmentSize",        maxSegmentSize,        22,  65535);
        checkValue("maxOutstandingSegs",    maxOutstandingSegs,    1,   255);
        checkValue("maxRetrans",            maxRetrans,            0,   255);
        checkValue("maxCumulativeAcks",     maxCumulativeAcks,     0,   255);
        checkValue("maxOutOfSequence",      maxOutOfSequence,      0,   255);
        checkValue("maxAutoReset",          maxAutoReset,          0,   255);
        checkValue("nullSegmentTimeout",    nullSegmentTimeout,    0,   65535);
        checkValue("retransmissionTimeout", retransmissionTimeout, 100, 65535);
        checkValue("cumulativeAckTimeout",  cumulativeAckTimeout,  100, 65535);

        this.maxSendQueueSize = maxSendQueueSize;
        this.maxRecvQueueSize = maxRecvQueueSize;
        this.maxSegmentSize = maxSegmentSize;
        this.maxOutstandingSegs = maxOutstandingSegs;
        this.maxRetrans = maxRetrans;
        this.maxCumulativeAcks = maxCumulativeAcks;
        this.maxOutOfSequence = maxOutOfSequence;
        this.maxAutoReset = maxAutoReset;
        this.nullSegmentTimeout = nullSegmentTimeout;
        this.retransmissionTimeout = retransmissionTimeout;
        this.cumulativeAckTimeout = cumulativeAckTimeout;
    }

    /**
     * Returns the maximum send queue size (packets).
     */
    public int maxSendQueueSize() {
        return maxSendQueueSize;
    }

    /**
     * Returns the maximum receive queue size (packets).
     */
    public int maxRecvQueueSize() {
        return maxRecvQueueSize;
    }

    /**
     * Returns the maximum segment size (octets).
     */
    public int maxSegmentSize() {
        return maxSegmentSize;
    }

    /**
     * Returns the maximum number of outstanding segments.
     */
    public int maxOutstandingSegs() {
        return maxOutstandingSegs;
    }

    /**
     * Returns the maximum number of consecutive retransmissions (0 means unlimited).
     */
    public int maxRetrans() {
        return maxRetrans;
    }

    /**
     * Returns the maximum number of unacknowledged received segments.
     */
    public int maxCumulativeAcks() {
        return maxCumulativeAcks;
    }

    /**
     * Returns the maximum number of out-of-sequence received segments.
     */
    public int maxOutOfSequence() {
        return maxOutOfSequence;
    }

    /**
     * Returns the maximum number of consecutive auto resets.
     */
    public int maxAutoReset() {
        return maxAutoReset;
    }

    /**
     * Returns the null segment timeout (ms).
     */
    public int nullSegmentTimeout() {
        return nullSegmentTimeout;
    }

    /**
     * Returns the retransmission timeout (ms).
     */
    public int retransmissionTimeout() {
        return retransmissionTimeout;
    }

    /**
     * Returns the cumulative acknowledge timeout (ms).
     */
    public int cumulativeAckTimeout() {
        return cumulativeAckTimeout;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(maxSendQueueSize).append(", ");
        sb.append(maxRecvQueueSize).append(", ");
        sb.append(maxSegmentSize).append(", ");
        sb.append(maxOutstandingSegs).append(", ");
        sb.append(maxRetrans).append(", ");
        sb.append(maxCumulativeAcks).append(", ");
        sb.append(maxOutOfSequence).append(", ");
        sb.append(maxAutoReset).append(", ");
        sb.append(nullSegmentTimeout).append(", ");
        sb.append(retransmissionTimeout).append(", ");
        sb.append(cumulativeAckTimeout);
        sb.append("]");
        return sb.toString();
    }

    private void checkValue(String param,
                                 int value,
                                 int minValue,
                                 int maxValue) {
        if (value < minValue || value > maxValue) {
            throw new IllegalArgumentException(param);
        }
    }

    private final int maxSendQueueSize;
    private final int maxRecvQueueSize;
    private final int maxSegmentSize;
    private final int maxOutstandingSegs;
    private final int maxRetrans;
    private final int maxCumulativeAcks;
    private final int maxOutOfSequence;
    private final int maxAutoReset;
    private final int nullSegmentTimeout;
    private final int retransmissionTimeout;
    private final int cumulativeAckTimeout;
}

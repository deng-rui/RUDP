package net.udp.impl;

/*
 *  RST Segment
 *
 *   0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | |A| | | | | | |               |
 *  |0|C|0|1|0|0|0|0|        6      |
 *  | |K| | | | | | |               |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |         Header Checksum       |
 *  +---------------+---------------+
 *
 */
public class RSTSegment extends Segment {
    protected RSTSegment() {
    }

    public RSTSegment(int seqn) {
        init(RST_FLAG, seqn, RUDP_HEADER_LEN);
    }

    @Override
    public String type() {
        return "RST";
    }
}

package net.udp.impl;



/*
 *  NUL Segment
 *
 *   0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |0|1|0|0|1|0|0|0|       6       |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |  Ack Number   |
 *  +---------------+---------------+
 *  |            Checksum           |
 *  +---------------+---------------+
 *
 */
public class NULSegment extends Segment {
    protected NULSegment() {
    }

    public NULSegment(int seqn) {
        init(NUL_FLAG, seqn, RUDP_HEADER_LEN);
    }

    @Override
    public String type() {
        return "NUL";
    }
}

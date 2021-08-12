package net.udp.impl;



/*
 *  ACK Segment
 *
 *   0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |0|1|0|0|0|0|0|0|       6       |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |           Checksum            |
 *  +---------------+---------------+
 *
 */
public class ACKSegment extends Segment {
    protected ACKSegment() {
    }

    public ACKSegment(int seqn, int ackn) {
        init(ACK_FLAG, seqn, RUDP_HEADER_LEN);
        setAck(ackn);
    }

    @Override
    public String type() {
        return "ACK";
    }
}

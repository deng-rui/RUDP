package net.udp.impl;



/*
 *  EACK Segment
 *
 *   0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |0|1|1|0|0|0|0|0|     N + 6     |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |1st out of seq |2nd out of seq |
 *  |  ack number   |   ack number  |
 *  +---------------+---------------+
 *  |  . . .        |Nth out of seq |
 *  |               |   ack number  |
 *  +---------------+---------------+
 *  |            Checksum           |
 *  +---------------+---------------+
 *
 */
public class EAKSegment extends ACKSegment {
    protected EAKSegment() {
    }

    public EAKSegment(int seqn, int ackn,  int[] acks) {
        init(EAK_FLAG, seqn, RUDP_HEADER_LEN + acks.length);
        setAck(ackn);
        _acks = acks;
    }

    @Override
    public String type() {
        return "EAK";
    }

    public int[] getACKs() {
        return _acks;
    }

    @Override
    public byte[] getBytes() {
        byte[] buffer = super.getBytes();

        for (int i = 0; i < _acks.length; i++) {
            buffer[4+i] = (byte) (_acks[i] & 0xFF);
        }

        return buffer;
    }

    @Override
    protected void parseBytes(byte[] buffer, int off, int len) {
        super.parseBytes(buffer, off, len);
        _acks = new int[len - RUDP_HEADER_LEN];
        for (int i = 0; i < _acks.length; i++) {
            _acks[i] = (buffer[off + 4 + i] & 0xFF);
        }
    }

    private int[] _acks;
}

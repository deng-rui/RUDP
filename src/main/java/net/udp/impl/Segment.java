package net.udp.impl;

public abstract class Segment {
    public static final int RUDP_VERSION = 1;
    public static final int RUDP_HEADER_LEN = 6;

    public static final byte SYN_FLAG = (byte) 0x80;
    public static final byte ACK_FLAG = (byte) 0x40;
    public static final byte EAK_FLAG = (byte) 0x20;
    public static final byte RST_FLAG = (byte) 0x10;
    public static final byte NUL_FLAG = (byte) 0x08;
    public static final byte CHK_FLAG = (byte) 0x04;
    public static final byte FIN_FLAG = (byte) 0x02;

    /** Control flags field */
    private int flags;
    /** Header length field */
    private int hlen;
    /** Sequence number field */
    private int seqn;
    /** Acknowledgment number field */
    private int ackn;
    /** Retransmission counter */
    private int nretx; 
    protected Segment() {
        this.nretx = 0;
        this.ackn = -1;
    }

    public abstract String type();

    public int flags() {
        return flags;
    }

    public int seq() {
        return seqn;
    }

    public int length() {
        return hlen;
    }

    public void setAck(int ackn) {
        this.flags = flags | ACK_FLAG;
        this.ackn = ackn;
    }

    public int getAck() {
        if ((flags & ACK_FLAG) == ACK_FLAG) {
            return ackn;
        }

        return -1;
    }

    public int getRetxCounter() {
        return nretx;
    }

    public void setRetxCounter(int n) {
        this.nretx = n;
    }

    public byte[] getBytes() {
        byte[] buffer = new byte[length()];

        buffer[0] = (byte) (flags & 0xFF);
        buffer[1] = (byte) (hlen & 0xFF);
        buffer[2] = (byte) (seqn & 0xFF);
        buffer[3] = (byte) (ackn & 0xFF);

        return buffer;
    }

    @Override
    public String toString() {
        return type() +
        " [" +
        " SEQ = " + seq() +
        ", ACK = " + ((getAck() >= 0) ? ""+getAck() : "N/A") +
        ", LEN = " + length() +
        " ]";
    }

    public static Segment parse(byte[] bytes) {
        return Segment.parse(bytes, 0, bytes.length);
    }

    public static Segment parse(byte[] bytes, int off, int len) {
        Segment segment = null;

        if (len < RUDP_HEADER_LEN) {
            throw new IllegalArgumentException("Invalid segment");
        }

        int flags = bytes[off];
        if ((flags & SYN_FLAG) != 0) {
            segment = new SYNSegment();
        } else if ((flags & NUL_FLAG) != 0) {
            segment = new NULSegment();
        } else if ((flags & EAK_FLAG) != 0) {
            segment = new EAKSegment();
        } else if ((flags & RST_FLAG) != 0) {
            segment = new RSTSegment();
        } else if ((flags & FIN_FLAG) != 0) {
            segment = new FINSegment();
        } else if ((flags & ACK_FLAG) != 0) { /* always process ACKs or Data segments last */
            if (len == RUDP_HEADER_LEN) {
                segment = new ACKSegment();
            }
            else {
                segment = new DATSegment();
            }
        }

        if (segment == null) {
            throw new IllegalArgumentException("Invalid segment");
        }

        segment.parseBytes(bytes, off, len);
        return segment;
    }

    /*
     *  RUDP Header
     *
     *   0 1 2 3 4 5 6 7 8            15
     *  +-+-+-+-+-+-+-+-+---------------+
     *  |S|A|E|R|N|C| | |    Header     |
     *  |Y|C|A|S|U|H|0|0|    Length     |
     *  |N|K|K|T|L|K| | |               |
     *  +-+-+-+-+-+-+-+-+---------------+
     *  |  Sequence #   +   Ack Number  |
     *  +---------------+---------------+
     *  |            Checksum           |
     *  +---------------+---------------+
     *
     */
    protected void init(int flags, int seqn, int len) {
        this.flags = flags;
        this.seqn = seqn;
        this.hlen = len;
    }

    protected void parseBytes(byte[] buffer, int off, int len) {
        this.flags = (buffer[off] & 0xFF);
        this.hlen  = (buffer[off+1] & 0xFF);
        this.seqn  = (buffer[off+2] & 0xFF);
        this.ackn  = (buffer[off+3] & 0xFF);
    }
}

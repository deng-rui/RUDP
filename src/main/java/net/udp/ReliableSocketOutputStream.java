package net.udp;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class extends OutputStream to implement a ReliableSocketOutputStream.
 * Note that this class should <b>NOT</b> be public.
 *
 * @author Adrian Granados
 *
 */
class ReliableSocketOutputStream extends OutputStream {
    protected ReliableSocket sock;
    protected byte[]         buf;
    protected int            count;

    /**
     * Creates a new ReliableSocketOutputStream.
     * This method can only be called by a ReliableSocket.
     *
     * @param sock    the actual RUDP socket to writes bytes on.
     * @throws IOException if an I/O error occurs.
     */
    public ReliableSocketOutputStream(ReliableSocket sock) throws IOException {
        if (sock == null) {
            throw new NullPointerException("sock");
        }

        this.sock = sock;
        buf = new byte[sock.getSendBufferSize()];
        count = 0;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (count >= buf.length) {
            flush();
        }

        buf[count++] = (byte) (b & 0xFF);
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }

        if (off < 0 || len < 0 || (off+len) > b.length) {
            throw new IndexOutOfBoundsException();
        }

        int buflen;
        int writtenBytes = 0;

        while (writtenBytes < len) {
            buflen = Math.min(buf.length, len-writtenBytes);
            if (buflen > (buf.length - count)) {
                flush();
            }
            System.arraycopy(b, off+writtenBytes, buf, count, buflen);
            count += buflen;
            writtenBytes += buflen;
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        if (count > 0) {
            sock.write(buf, 0, count);
            count = 0;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        sock.shutdownOutput();
    }
}

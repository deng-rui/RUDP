package net.udp;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class extends InputStream to implement a ReliableSocketInputStream.
 * Note that this class should <b>NOT</b> be public.
 *
 * @author Adrian Granados
 * @author Dr
 *
 */
public class ReliableSocketInputStream extends InputStream {
	protected ReliableSocket sock;
	protected byte[] buf;
	protected int pos;
	protected int count;

	/**
	 * Creates a new ReliableSocketInputStream.
	 * This method can only be called by a ReliableSocket.
	 *
	 * @param sock the actual RUDP socket to read bytes on.
	 * @throws IOException if an I/O error occurs.
	 */
	public ReliableSocketInputStream(ReliableSocket sock) throws IOException {
		if (sock == null) {
			throw new NullPointerException("sock");
		}

		this.sock = sock;
		buf = new byte[sock.getReceiveBufferSize()];
		pos = count = 0;
	}

	@Override
	public synchronized int read() throws IOException {
		if (readImpl() < 0) {
			return -1;
		}

		return (buf[pos++] & 0xFF);
	}

	@Override
	public synchronized int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || (off + len) > b.length) {
			throw new IndexOutOfBoundsException();
		}

		if (readImpl() < 0) {
			return -1;
		}

		int readBytes = Math.min(available(), len);
		System.arraycopy(buf, pos, b, off, readBytes);
		pos += readBytes;

		return readBytes;
	}

	@Override
	public synchronized int available() {
		return (count - pos);
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public void close() throws IOException {
		sock.shutdownInput();
	}

	public int getReadsDataLength() throws IOException {
		return sock.read(buf, 0, buf.length);
	}

	public byte[] getReadsData() throws IOException {
		return buf;
	}

	private int readImpl() throws IOException {
		if (available() == 0) {
			count = sock.read(buf, 0, buf.length);
			pos = 0;
		}

		return count;
	}
}

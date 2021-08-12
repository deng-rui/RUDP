package net.udp;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class extends InputStream to implement a ReliableSocketInputStream.
 * Note that this class should <b>NOT</b> be public.
 *
 * @author Adrian Granados
 *
 */
class ReliableSocketInputStream extends InputStream {
	/**
	 * Creates a new ReliableSocketInputStream.
	 * This method can only be called by a ReliableSocket.
	 *
	 * @param sock
	 *            the actual RUDP socket to read bytes on.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public ReliableSocketInputStream(ReliableSocket sock) throws IOException {
		if (sock == null) {
			throw new NullPointerException("sock");
		}

		_sock = sock;
		_buf = new byte[_sock.getReceiveBufferSize()];
		_pos = _count = 0;
	}

	@Override
	public synchronized int read() throws IOException {
		if (readImpl() < 0) {
			return -1;
		}

		return (_buf[_pos++] & 0xFF);
	}

	@Override
	public synchronized int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		if (b == null) {
			throw new NullPointerException();
		}

		if (off < 0 || len < 0 || (off + len) > b.length) {
			throw new IndexOutOfBoundsException();
		}

		if (readImpl() < 0) {
			return -1;
		}

		int readBytes = Math.min(available(), len);
		System.arraycopy(_buf, _pos, b, off, readBytes);
		_pos += readBytes;

		return readBytes;
	}

	@Override
	public synchronized int available() {
		return (_count - _pos);
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public void close() throws IOException {
		_sock.shutdownInput();
	}

	private int readImpl() throws IOException {
		if (available() == 0) {
			_count = _sock.read(_buf, 0, _buf.length);
			_pos = 0;
		}

		return _count;
	}

	protected ReliableSocket _sock;
	protected byte[] _buf;
	protected int _pos;
	protected int _count;
}

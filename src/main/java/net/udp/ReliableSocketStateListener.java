package net.udp;

/**
 * The listener interface for receiving socket events.
 * The class that is interested in processing a socket
 * event implements this interface.
 *
 * @author Adrian Granados
 *
 */
public interface ReliableSocketStateListener {
    /**
     * Invoked when the connection is opened.
     */
    void connectionOpened(ReliableSocket sock);

    /**
     * Invoked when the connection is closed.
     */
    void connectionClosed(ReliableSocket sock);

    /**
     * Invoked when the (established) connection fails.
     */
    void connectionFailure(ReliableSocket sock);
}

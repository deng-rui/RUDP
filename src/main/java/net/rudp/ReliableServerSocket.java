package net.udp;

import net.udp.impl.SYNSegment;
import net.udp.impl.Segment;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class implements server sockets that use
 * the Simple Reliable UDP (RUDP) protocol.
 *
 * @author Adrian Granados
 * @see java.net.ServerSocket
 */
public class ReliableServerSocket extends ServerSocket {
    /**
     * Creates an unbound RUDP server socket.
     *
     * @throws IOException if an I/O error occurs when opening
     *         the underlying UDP socket.
     * @see java.net.ServerSocket#ServerSocket()
     */
    public ReliableServerSocket() throws IOException {
        this(0, 0, null);
    }

    /**
     * Creates a RUDP server socket, bound to the specified port. A port
     * of <code>0</code> creates a socket on any free port.
     * </p>
     * The maximum queue length for incoming connection indications (a
     * request to connect) is set to <code>50</code>. If a connection
     * indication arrives when the queue is full, the connection is refused.
     *
     * @param  port    the port number, or <code>0</code> to use any free port.
     * @throws IOException if an I/O error occurs when opening
     *         the underlying UDP socket.
     * @see java.net.ServerSocket#ServerSocket(int)
     */
    public ReliableServerSocket(int port) throws IOException {
        this(port, 0, null);
    }

    /**
     * Creates a RUDP server socket and binds it to the specified local port, with
     * the specified backlog. A port of <code>0</code> creates a socket on any
     * free port.
     *
     * @param port      the port number, or <code>0</code> to use any free port.
     * @param backlog   the listen backlog.
     * @throws IOException if an I/O error occurs when opening
     *         the underlying UDP socket.
     * @see java.net.ServerSocket#ServerSocket(int, int)
     */
    public ReliableServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }

    /**
     * Creates a RUDP server socket and binds it to the specified local port and
     * IP address, with the specified backlog. The <i>bindAddr</i> argument
     * can be used on a multi-homed host for a ReliableServerSocket that
     * will only accept connect requests to one of its addresses.
     * If <i>bindAddr</i> is null, it will default accepting
     * connections on any/all local addresses.
     * A port of <code>0</code> creates a socket on any free port.
     *
     * @param port      the port number, or <code>0</code> to use any free port.
     * @param backlog   the listen backlog.
     * @param bindAddr  the local InetAddress the server will bind to.
     * @throws IOException if an I/O error occurs when opening
     *         the underlying UDP socket.
     * @see java.net.ServerSocket#ServerSocket(int, int, InetAddress)
     */
    public ReliableServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        this(new DatagramSocket(new InetSocketAddress(bindAddr, port)), backlog);
    }

    /**
     * Creates a RUDP server socket attached to the specified UDP socket, with
     * the specified backlog.
     *
     * @param sock    the underlying UDP socket.
     * @param backlog the listen backlog.
     * @throws IOException if an I/O error occurs.
     */
    public ReliableServerSocket(DatagramSocket sock, int backlog) throws IOException {
        if (sock == null) {
            throw new NullPointerException("sock");
        }

        _serverSock = sock;
        int _backlogSize = (backlog <= 0) ? DEFAULT_BACKLOG_SIZE : backlog;
        _backlog = new ArrayList<>(_backlogSize);
        _clientSockTable = new HashMap<>();
        _stateListener = new StateListener();
        _timeout = 0;
        _closed = false;

        new ReceiverThread().start();
    }

    @Override
    public Socket accept() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        synchronized (_backlog) {
            while (_backlog.isEmpty()) {
                try {
                    if (_timeout == 0) {
                        _backlog.wait();
                    }
                    else {
                        long startTime = System.currentTimeMillis();
                        _backlog.wait(_timeout);
                        if (System.currentTimeMillis() - startTime >= _timeout) {
                            throw new SocketTimeoutException();
                        }
                    }

                }
                catch (InterruptedException xcp) {
                    xcp.printStackTrace();
                }

                if (isClosed()) {
                    throw new IOException();
                }
            }

            return _backlog.remove(0);
        }
    }

    @Override
    public synchronized void bind(SocketAddress endpoint) throws IOException {
        bind(endpoint, 0);
    }

    @Override
    public synchronized void bind(SocketAddress endpoint, int backlog) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        _serverSock.bind(endpoint);
    }

    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        _closed = true;
        synchronized (_backlog) {
            _backlog.clear();
            _backlog.notify();
        }

        if (_clientSockTable.isEmpty()) {
            _serverSock.close();
        }
    }

    @Override
    public InetAddress getInetAddress() {
        return _serverSock.getInetAddress();
    }

    @Override
    public int getLocalPort() {
        return _serverSock.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return _serverSock.getLocalSocketAddress();
    }

    @Override
    public boolean isBound() {
        return _serverSock.isBound();
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }

    @Override
    public void setSoTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }

        _timeout = timeout;
    }

    @Override
    public int getSoTimeout() {
        return _timeout;
    }

    /**
     * Registers a new client socket with the specified endpoint address.
     *
     * @param endpoint    the new socket.
     * @return the registered socket.
     */
    private ReliableClientSocket addClientSocket(SocketAddress endpoint) {
        synchronized (_clientSockTable) {
            ReliableClientSocket sock = _clientSockTable.get(endpoint);

            if (sock == null) {
                try {
                    sock = new ReliableClientSocket(_serverSock, endpoint);
                    sock.addStateListener(_stateListener);
                    _clientSockTable.put(endpoint, sock);
                }
                catch (IOException xcp) {
                    xcp.printStackTrace();
                }
            }

            return sock;
        }
    }

    /**
     * Deregisters a client socket with the specified endpoint address.
     *
     * @param endpoint     the socket.
     * @return the deregistered socket.
     */
    private ReliableClientSocket removeClientSocket(SocketAddress endpoint) {
        synchronized (_clientSockTable) {
            ReliableClientSocket sock = _clientSockTable.remove(endpoint);

            if (_clientSockTable.isEmpty()) {
                if (isClosed()) {
                    _serverSock.close();
                }
            }

            return sock;
        }
    }

    private final DatagramSocket _serverSock;
    private int            _timeout;
    private boolean        _closed;

    /*
     * The listen backlog queue.
     */
    private final ArrayList<net.udp.ReliableSocket>      _backlog;

    /*
     * A table of active opened client sockets.
     */
    private final HashMap<SocketAddress, ReliableClientSocket>   _clientSockTable;

    private final net.udp.ReliableSocketStateListener _stateListener;

    private static final int DEFAULT_BACKLOG_SIZE = 50;

    private class ReceiverThread extends Thread {
        public ReceiverThread()
        {
            super("ReliableServerSocket");
            setDaemon(true);
        }

        @Override
        public void run()
        {
            byte[] buffer = new byte[65535];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                ReliableClientSocket sock = null;

                try {
                    _serverSock.receive(packet);
                    SocketAddress endpoint = packet.getSocketAddress();
                    Segment s = Segment.parse(packet.getData(), 0, packet.getLength());

                    synchronized (_clientSockTable) {

                        if (!isClosed()) {
                            if (s instanceof SYNSegment) {
                                if (!_clientSockTable.containsKey(endpoint)) {
                                    sock = addClientSocket(endpoint);
                                }
                            }
                        }

                        sock = _clientSockTable.get(endpoint);
                    }

                    if (sock != null) {
                        sock.segmentReceived(s);
                    }
                }
                catch (IOException xcp) {
                    if (isClosed()) {
                        break;
                    }
                }
            }
        }
    }

    public static class ReliableClientSocket extends net.udp.ReliableSocket {
        public ReliableClientSocket(DatagramSocket sock,
                                    SocketAddress endpoint)
            throws IOException
        {
            super(sock);
            _endpoint = endpoint;
        }

        @Override
        protected void init(DatagramSocket sock, net.udp.ReliableSocketProfile profile)
        {
            _queue = new ArrayList<>();
            super.init(sock, profile);
        }

        @Override
        protected Segment receiveSegmentImpl()
        {
            synchronized (_queue) {
                while (_queue.isEmpty()) {
                    try {
                        _queue.wait();
                    }
                    catch (InterruptedException xcp) {
                        xcp.printStackTrace();
                    }
                }

                return _queue.remove(0);
            }
        }

        protected void segmentReceived(Segment s)
        {
            synchronized (_queue) {
                _queue.add(s);
                _queue.notify();
            }
        }

        @Override
        protected void closeSocket()
        {
            synchronized (_queue) {
                _queue.clear();
                _queue.add(null);
                _queue.notify();
            }
        }

        @Override
        protected void log(String msg)
        {
            System.out.println(getPort() + ": " + msg);
        }

        private ArrayList<Segment> _queue;
    }

    private class StateListener implements net.udp.ReliableSocketStateListener {
        @Override
        public void connectionOpened(net.udp.ReliableSocket sock)
        {
            if (sock instanceof ReliableClientSocket) {
                synchronized (_backlog) {
                    while (_backlog.size() > DEFAULT_BACKLOG_SIZE) {
                        try {
                            _backlog.wait();
                        }
                        catch (InterruptedException xcp) {
                            xcp.printStackTrace();
                        }
                    }

                    _backlog.add(sock);
                    _backlog.notify();
                }
            }
        }

        @Override
        public void connectionRefused(net.udp.ReliableSocket sock)
        {
            // do nothing.
        }

        @Override
        public void connectionClosed(net.udp.ReliableSocket sock)
        {
            // Remove client socket from the table of active connections.
            if (sock instanceof ReliableClientSocket) {
                removeClientSocket(sock.getRemoteSocketAddress());
            }
        }

        @Override
        public void connectionFailure(net.udp.ReliableSocket sock)
        {
            // Remove client socket from the table of active connections.
            if (sock instanceof ReliableClientSocket) {
                removeClientSocket(sock.getRemoteSocketAddress());
            }
        }

        @Override
        public void connectionReset(net.udp.ReliableSocket sock)
        {
            // do nothing.
        }
    }
}

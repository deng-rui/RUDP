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

        serverSock = sock;
        int backlogSize = (backlog <= 0) ? DEFAULT_BACKLOG_SIZE : backlog;
        this.backlog = new ArrayList<>(backlogSize);
        clientSockTable = new HashMap<>();
        _stateListener = new StateListener();
        timeout = 0;
        closed = false;

        new ReceiverThread().start();
    }

    @Override
    public Socket accept() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        synchronized (backlog) {
            while (backlog.isEmpty()) {
                try {
                    if (timeout == 0) {
                        backlog.wait();
                    }
                    else {
                        long startTime = System.currentTimeMillis();
                        backlog.wait(timeout);
                        if (System.currentTimeMillis() - startTime >= timeout) {
                            throw new SocketTimeoutException();
                        }
                    }

                } catch (InterruptedException xcp) {
                    xcp.printStackTrace();
                }

                if (isClosed()) {
                    throw new IOException();
                }
            }

            return backlog.remove(0);
        }
    }

    @Override
    public synchronized void bind(SocketAddress endpoint) throws IOException {
        //用不上 API保留
    }

    @Override
    public synchronized void bind(SocketAddress endpoint, int backlog) throws IOException {
        //用不上 API保留
    }

    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        closed = true;
        synchronized (backlog) {
            backlog.clear();
            backlog.notify();
        }

        if (clientSockTable.isEmpty()) {
            serverSock.close();
        }
    }

    @Override
    public InetAddress getInetAddress() {
        return serverSock.getInetAddress();
    }

    @Override
    public int getLocalPort() {
        return serverSock.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return serverSock.getLocalSocketAddress();
    }

    @Override
    public boolean isBound() {
        return serverSock.isBound();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void setSoTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }

        this.timeout = timeout;
    }

    @Override
    public int getSoTimeout() {
        return timeout;
    }

    /**
     * Registers a new client socket with the specified endpoint address.
     *
     * @param endpoint    the new socket.
     * @return the registered socket.
     */
    private ReliableClientSocket addClientSocket(SocketAddress endpoint) {
        synchronized (clientSockTable) {
            ReliableClientSocket sock = clientSockTable.get(endpoint);

            if (sock == null) {
                try {
                    sock = new ReliableClientSocket(serverSock, endpoint);
                    sock.addStateListener(_stateListener);
                    clientSockTable.put(endpoint, sock);
                } catch (IOException xcp) {
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
     */
    private void removeClientSocket(SocketAddress endpoint) {
        synchronized (clientSockTable) {
            clientSockTable.remove(endpoint);

            if (clientSockTable.isEmpty()) {
                if (isClosed()) {
                    serverSock.close();
                }
            }

        }
    }

    private final DatagramSocket serverSock;
    private int            timeout;
    private boolean        closed;

    /**
     * The listen backlog queue.
     */
    private final ArrayList<net.udp.ReliableSocket>      backlog;

    /**
     * A table of active opened client sockets.
     */
    private final HashMap<SocketAddress, ReliableClientSocket>   clientSockTable;

    private final net.udp.ReliableSocketStateListener _stateListener;

    private static final int DEFAULT_BACKLOG_SIZE = 50;

    private class ReceiverThread extends Thread {
        public ReceiverThread() {
            super("ReliableServerSocket");
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] buffer = new byte[65535];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                ReliableClientSocket sock;

                try {
                    serverSock.receive(packet);
                    SocketAddress endpoint = packet.getSocketAddress();
                    Segment s = Segment.parse(packet.getData(), 0, packet.getLength());

                    synchronized (clientSockTable) {

                        if (!isClosed()) {
                            if (s instanceof SYNSegment) {
                                if (!clientSockTable.containsKey(endpoint)) {
                                    sock = addClientSocket(endpoint);
                                }
                            }
                        }

                        sock = clientSockTable.get(endpoint);
                    }

                    if (sock != null) {
                        sock.segmentReceived(s);
                    }
                } catch (IOException xcp) {
                    if (isClosed()) {
                        break;
                    }
                }
            }
        }
    }

    public static class ReliableClientSocket extends ReliableSocket {
        public ReliableClientSocket(DatagramSocket sock, SocketAddress endpoint) throws IOException {
            super(sock);
            _endpoint = endpoint;
        }

        @Override
        protected void init(DatagramSocket sock, net.udp.ReliableSocketProfile profile) {
            queue = new ArrayList<>();
            super.init(sock, profile);
        }

        @Override
        protected Segment receiveSegmentImpl() {
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException xcp) {
                        xcp.printStackTrace();
                    }
                }

                return queue.remove(0);
            }
        }

        protected void segmentReceived(Segment s) {
            synchronized (queue) {
                queue.add(s);
                queue.notify();
            }
        }

        @Override
        protected void closeSocket() {
            synchronized (queue) {
                queue.clear();
                queue.add(null);
                queue.notify();
            }
        }

        @Override
        protected void log(String msg) {
            System.out.println(getPort() + ": " + msg);
        }

        private ArrayList<Segment> queue;
    }

    private class StateListener implements net.udp.ReliableSocketStateListener {
        @Override
        public void connectionOpened(net.udp.ReliableSocket sock) {
            if (sock instanceof ReliableClientSocket) {
                synchronized (backlog) {
                    while (backlog.size() > DEFAULT_BACKLOG_SIZE) {
                        try {
                            backlog.wait();
                        }
                        catch (InterruptedException xcp) {
                            xcp.printStackTrace();
                        }
                    }

                    backlog.add(sock);
                    backlog.notify();
                }
            }
        }

        @Override
        public void connectionClosed(net.udp.ReliableSocket sock) {
            // Remove client socket from the table of active connections.
            if (sock instanceof ReliableClientSocket) {
                removeClientSocket(sock.getRemoteSocketAddress());
            }
        }

        @Override
        public void connectionFailure(net.udp.ReliableSocket sock) {
            // Remove client socket from the table of active connections.
            if (sock instanceof ReliableClientSocket) {
                removeClientSocket(sock.getRemoteSocketAddress());
            }
        }
    }
}

package com.openflux.sdk;

import java.net.Socket;

/** Called for every outbound SDK socket BEFORE it is connected. */
public interface SocketProtector {
    /** @return true if the socket was excluded from the VPN. */
    boolean protect(Socket socket);
}

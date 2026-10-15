package com.openflux.sdk;

/** Connection parameters for {@link OpenFluxClient}. */
public final class ClientConfig {
    public String host;
    public int port;
    /** "tcp" (plain framing) or "tls". */
    public String transport = "tcp";
    public String authToken = "";
    public String publicKey = "";
    public String privateKey = "";
    public String sni = "";
    public String alpn = "";
    public int mtu = 1400;
    public int connectTimeoutMs = 10000;
    public int readTimeoutMs = 30000;
    public int keepaliveMs = 15000;
}

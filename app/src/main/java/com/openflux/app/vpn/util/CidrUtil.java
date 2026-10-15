package com.openflux.app.vpn.util;

/** Разбор CIDR вида "10.0.0.0/8" или "::/0". */
public final class CidrUtil {
    private CidrUtil() {}

    public static final class Cidr {
        public final String address;
        public final int prefix;

        Cidr(String address, int prefix) {
            this.address = address;
            this.prefix = prefix;
        }
    }

    /** @return разобранный CIDR или null, если формат неверный */
    public static Cidr parse(String cidr) {
        if (cidr == null) return null;
        String[] parts = cidr.trim().split("/");
        if (parts.length != 2 || parts[0].isEmpty()) return null;
        boolean v6 = parts[0].contains(":");
        if (!v6 && !parts[0].matches("\\d{1,3}(\\.\\d{1,3}){3}")) return null;
        if (!v6) {
            for (String o : parts[0].split("\\.")) {
                if (Integer.parseInt(o) > 255) return null;
            }
        }
        try {
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > (v6 ? 128 : 32)) return null;
            return new Cidr(parts[0], prefix);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

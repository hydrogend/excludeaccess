package dev.hydrogen1.excludeaccess.util;

import inet.ipaddr.IPAddressString;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class IPFilter {
    private final List<IPAddressString> masks;
    public IPFilter(@NonNull List<String> masks) {
        Objects.requireNonNull(masks);

        this.masks = masks.stream()
                .filter(Predicate.not(String::isEmpty))
                .map(IPAddressString::new)
                .toList();
    }
    public boolean isRegistered(InetAddress ip) {
        val addr = new IPAddressString(ip.getHostAddress());
        return masks.stream().anyMatch(m -> m.contains(addr));
    }
}

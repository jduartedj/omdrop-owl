// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Bounded streaming local-file receipt. Ownership of input remains with caller. */
public final class FileDigest {
    public record Receipt(long bytes, String sha256) {}
    private FileDigest() {}
    public static Receipt hash(InputStream input, long limit) throws IOException {
        if (input == null || limit < 0) throw new IllegalArgumentException("invalid input or bound");
        final MessageDigest md;
        try { md = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        long bytes = 0;
        byte[] buffer = new byte[65536];
        while (true) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("cancelled");
            int count = input.read(buffer, 0, (int) Math.min(buffer.length, Math.min(limit - bytes, buffer.length - 1) + 1));
            if (count < 0) break;
            if (count == 0) {
                int one = input.read();
                if (one < 0) break;
                buffer[0] = (byte) one;
                count = 1;
            }
            if (count > limit - bytes) throw new IOException("file exceeds local hashing limit");
            md.update(buffer, 0, count);
            bytes += count;
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte value : md.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return new Receipt(bytes, hex.toString());
    }
}

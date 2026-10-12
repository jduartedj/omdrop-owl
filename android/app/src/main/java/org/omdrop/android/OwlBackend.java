// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;

/** Adapter seam only. No implementation exists in this stock-Android artifact. */
public interface OwlBackend {
    boolean implemented();
    void start();
    void send();
    void receive();

    final class Unavailable implements OwlBackend {
        @Override public boolean implemented() { return false; }
        @Override public void start() { throw blocked(); }
        @Override public void send() { throw blocked(); }
        @Override public void receive() { throw blocked(); }
        private UnsupportedOperationException blocked() {
            return new UnsupportedOperationException("Native OWL/AirDrop unavailable; no LAN fallback");
        }
    }
}

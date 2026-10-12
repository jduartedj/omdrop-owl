// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;
import java.util.concurrent.atomic.AtomicLong;
/** Prevent cancelled or superseded asynchronous receipts from becoming UI state. */
public final class GenerationGate {
    private final AtomicLong current = new AtomicLong();
    public long next() { return current.incrementAndGet(); }
    public boolean owns(long value) { return current.get() == value; }
}

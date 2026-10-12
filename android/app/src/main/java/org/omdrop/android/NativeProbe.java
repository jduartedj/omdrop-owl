// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;

/** Read-only NDK capability diagnostic, not an OWL engine or privilege broker. */
public final class NativeProbe {
    public record Snapshot(boolean loaded, boolean capsReadable, boolean netAdmin,
                           boolean netRaw, boolean packetSocket, int packetErrno,
                           boolean genericNetlinkSocket, int netlinkErrno, int selinuxEnforcing) {}
    private static final boolean LOADED;
    static {
        boolean loaded;
        try { System.loadLibrary("omdrop_probe"); loaded = true; }
        catch (UnsatisfiedLinkError | SecurityException unavailable) { loaded = false; }
        LOADED = loaded;
    }
    private NativeProbe() {}
    private static native long[] readOnlyProbe();
    public static Snapshot inspect() {
        if (!LOADED) return new Snapshot(false, false, false, false, false, -1, false, -1, -1);
        try {
            long[] a = readOnlyProbe();
            if (a == null || a.length != 8) return new Snapshot(false, false, false, false, false, -1, false, -1, -1);
            return new Snapshot(true, a[0] == 1, a[1] == 1, a[2] == 1, a[3] == 1,
                    (int) a[4], a[5] == 1, (int) a[6], (int) a[7]);
        } catch (UnsatisfiedLinkError unavailable) {
            return new Snapshot(false, false, false, false, false, -1, false, -1, -1);
        }
    }
    public static CapabilityGate.Facts facts(Snapshot s) {
        CapabilityGate.Evidence raw = !s.loaded() || !s.capsReadable() ? CapabilityGate.Evidence.UNKNOWN
                : s.netRaw() && s.packetSocket() ? CapabilityGate.Evidence.VERIFIED : CapabilityGate.Evidence.ABSENT;
        CapabilityGate.Evidence admin = !s.loaded() || !s.capsReadable() ? CapabilityGate.Evidence.UNKNOWN
                : s.netAdmin() ? CapabilityGate.Evidence.VERIFIED : CapabilityGate.Evidence.ABSENT;
        return new CapabilityGate.Facts(raw, admin, CapabilityGate.Evidence.UNKNOWN,
                CapabilityGate.Evidence.UNKNOWN, CapabilityGate.Evidence.UNKNOWN, CapabilityGate.Evidence.UNKNOWN,
                CapabilityGate.Evidence.ABSENT, CapabilityGate.Evidence.ABSENT);
    }
}

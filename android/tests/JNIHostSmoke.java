// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;
/** Linux host JNI plumbing check, deliberately NOT Android-device acceptance. */
public final class JNIHostSmoke {
    public static void main(String[] args) {
        NativeProbe.Snapshot s = NativeProbe.inspect();
        if (!s.loaded() || !s.capsReadable()) throw new AssertionError("JNI probe did not load/read host CapEff");
        if (CapabilityGate.assess(NativeProbe.facts(s)).candidateForHardwareTest())
            throw new AssertionError("Missing backend incorrectly became a candidate");
        if (CapabilityGate.assess(NativeProbe.facts(s)).accepted())
            throw new AssertionError("Capability check incorrectly claimed acceptance");
        System.out.println("Host JNI probe passed: loaded=" + s.loaded() + ", CapEff readable=" + s.capsReadable()
                + ", CAP_NET_ADMIN=" + s.netAdmin() + ", CAP_NET_RAW=" + s.netRaw()
                + ", empty packet socket=" + s.packetSocket() + ", packet errno=" + s.packetErrno()
                + ", empty generic netlink=" + s.genericNetlinkSocket() + ", accepted=false");
    }
}

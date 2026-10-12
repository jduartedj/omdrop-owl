// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;

import java.util.ArrayList;
import java.util.List;

/** Necessary radio conditions, NOT proof of working AirDrop. Unknown means blocked. */
public final class CapabilityGate {
    public enum Evidence { UNKNOWN, ABSENT, VERIFIED }
    public record Facts(Evidence rawSocket, Evidence netAdmin, Evidence monitorRxTx,
                        Evidence tap, Evidence sharedWifi, Evidence channel,
                        Evidence owlBackend, Evidence airDropProtocol) {}
    public record Result(List<String> blockers) {
        public Result { blockers = List.copyOf(blockers); }
        public boolean candidateForHardwareTest() { return blockers.isEmpty(); }
        public boolean accepted() { return false; }
    }
    private CapabilityGate() {}
    public static Result assess(Facts facts) {
        List<String> blockers = new ArrayList<>();
        require(blockers, facts.rawSocket(), "CAP_NET_RAW / packet socket");
        require(blockers, facts.netAdmin(), "CAP_NET_ADMIN / interface administration");
        require(blockers, facts.monitorRxTx(), "driver monitor RX and frame injection");
        require(blockers, facts.tap(), "OWL TAP / IPv6 link");
        require(blockers, facts.sharedWifi(), "Wi-Fi-preserving shared radio operation");
        require(blockers, facts.channel(), "authorized AWDL channel 6 / 44 / 149");
        require(blockers, facts.owlBackend(), "OWL backend implementation");
        require(blockers, facts.airDropProtocol(), "AirDrop discovery / TLS / send / receive implementation");
        return new Result(blockers);
    }
    private static void require(List<String> out, Evidence value, String name) {
        if (value != Evidence.VERIFIED) out.add(name + ": " + (value == Evidence.ABSENT ? "unavailable" : "unverified"));
    }
}

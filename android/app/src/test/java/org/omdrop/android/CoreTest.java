// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CoreTest {
    private CapabilityGate.Facts facts(CapabilityGate.Evidence[] a) {
        return new CapabilityGate.Facts(a[0],a[1],a[2],a[3],a[4],a[5],a[6],a[7]);
    }
    @Test public void exhaustiveUnknownOrAbsentAlwaysBlocks() {
        for (int n=0;n<6561;n++) {
            int value=n, verified=0;
            CapabilityGate.Evidence[] a=new CapabilityGate.Evidence[8];
            for(int i=0;i<8;i++) { a[i]=CapabilityGate.Evidence.values()[value%3]; value/=3; if(a[i]==CapabilityGate.Evidence.VERIFIED) verified++; }
            CapabilityGate.Result r=CapabilityGate.assess(facts(a));
            assertEquals(8-verified,r.blockers().size());
            assertEquals(verified==8,r.candidateForHardwareTest());
            assertFalse(r.accepted());
        }
    }
    @Test public void blockersAreImmutable() {
        CapabilityGate.Result r=new CapabilityGate.Result(List.of("blocked"));
        assertThrows(UnsupportedOperationException.class,()->r.blockers().clear());
    }
    @Test public void noOwlBackendFailsEveryOperation() {
        OwlBackend b=new OwlBackend.Unavailable();
        assertFalse(b.implemented());
        assertThrows(UnsupportedOperationException.class,b::start);
        assertThrows(UnsupportedOperationException.class,b::send);
        assertThrows(UnsupportedOperationException.class,b::receive);
    }
    @Test public void digestKnownVector() throws Exception {
        FileDigest.Receipt r=FileDigest.hash(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)),3);
        assertEquals(3,r.bytes());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",r.sha256());
    }
    @Test public void emptyDigest() throws Exception {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",FileDigest.hash(new ByteArrayInputStream(new byte[0]),0).sha256());
    }
    @Test public void oversizedFileProducesNoReceipt() {
        assertThrows(IOException.class,()->FileDigest.hash(new ByteArrayInputStream(new byte[4]),3));
        assertThrows(IOException.class,()->FileDigest.hash(new ByteArrayInputStream(new byte[1]),0));
    }
    @Test public void interruptedHashStops() {
        Thread.currentThread().interrupt();
        try { assertThrows(InterruptedIOException.class,()->FileDigest.hash(new ByteArrayInputStream(new byte[1]),3)); }
        finally { Thread.interrupted(); }
    }
    @Test public void failingStreamDoesNotProduceReceipt() {
        InputStream in=new InputStream(){ public int read() throws IOException { throw new IOException("fixture"); } };
        assertThrows(IOException.class,()->FileDigest.hash(in,3));
    }
    @Test public void zeroLengthReadDoesNotSpin() throws Exception {
        InputStream in=new ByteArrayInputStream(new byte[]{1,2,3}) { @Override public synchronized int read(byte[] b,int o,int l){return 0;} };
        assertEquals(FileDigest.hash(new ByteArrayInputStream(new byte[]{1,2,3}),3),FileDigest.hash(in,3));
    }
    @Test public void negativeOrNullRejected() {
        assertThrows(IllegalArgumentException.class,()->FileDigest.hash(null,3));
        assertThrows(IllegalArgumentException.class,()->FileDigest.hash(new ByteArrayInputStream(new byte[0]),-1));
    }
    @Test public void largeMultiBufferBoundaries() throws Exception {
        byte[] a=new byte[131073]; java.util.Arrays.fill(a,(byte)42);
        assertEquals(a.length,FileDigest.hash(new ByteArrayInputStream(a),a.length).bytes());
        assertThrows(IOException.class,()->FileDigest.hash(new ByteArrayInputStream(a),a.length-1));
    }
    @Test public void hashingDoesNotCloseCallerStream() throws Exception {
        boolean[] closed={false};
        InputStream in=new ByteArrayInputStream(new byte[0]) { @Override public void close(){closed[0]=true;} };
        FileDigest.hash(in,0);assertFalse(closed[0]);
    }
    @Test public void unreadableProbeRemainsUnknown() {
        NativeProbe.Snapshot s=new NativeProbe.Snapshot(false,false,false,false,false,-1,false,-1,-1);
        CapabilityGate.Facts f=NativeProbe.facts(s);
        assertEquals(CapabilityGate.Evidence.UNKNOWN,f.rawSocket());
        assertEquals(CapabilityGate.Evidence.ABSENT,f.owlBackend());
        assertFalse(CapabilityGate.assess(f).candidateForHardwareTest());
    }
    @Test public void evenPrivilegedProbeCannotEnableTransfer() {
        NativeProbe.Snapshot s=new NativeProbe.Snapshot(true,true,true,true,true,0,true,0,0);
        assertFalse(CapabilityGate.assess(NativeProbe.facts(s)).candidateForHardwareTest());
    }
    @Test public void deniedRawSocketBlocksDespiteCapabilityBit() {
        NativeProbe.Snapshot s=new NativeProbe.Snapshot(true,true,true,true,false,13,true,0,1);
        assertEquals(CapabilityGate.Evidence.ABSENT,NativeProbe.facts(s).rawSocket());
    }
}

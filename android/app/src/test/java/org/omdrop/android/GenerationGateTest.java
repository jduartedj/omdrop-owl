// SPDX-License-Identifier: GPL-3.0-or-later
package org.omdrop.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class GenerationGateTest {
    @Test public void invalidationRejectsQueuedReceipt() {
        GenerationGate g=new GenerationGate();long old=g.next();assertTrue(g.owns(old));
        long cancellation=g.next();assertFalse(g.owns(old));assertTrue(g.owns(cancellation));
    }
    @Test public void newRequestDoesNotInheritOldOwnership() {
        GenerationGate g=new GenerationGate();long old=g.next();long newer=g.next();
        assertFalse(g.owns(old));assertTrue(g.owns(newer));
    }
}

/* SPDX-License-Identifier: GPL-3.0-or-later */
#include "probe_core.h"
#include <assert.h>
#include <stdio.h>
#include <stdint.h>
int main(void) {
    uint64_t out=123;
    assert(omdrop_parse_caps("\t0000000000000000\n", &out) && out==0);
    assert(omdrop_parse_caps("0000000000003000", &out) && out==UINT64_C(0x3000));
    assert(omdrop_has_cap(out,12) && omdrop_has_cap(out,13));
    assert(!omdrop_has_cap(out,11) && !omdrop_has_cap(out,64));
    assert(omdrop_parse_caps("FFFFFFFFFFFFFFFF\r\n",&out) && out==UINT64_MAX);
    assert(omdrop_has_cap(out,63));
    const char *bad[]={"","\n","-1","0x3000","10000000000000000","g","3000\n3000","3000 ","3000\rjunk"};
    for (unsigned i=0;i<sizeof(bad)/sizeof(bad[0]);i++) {
        out=123; assert(!omdrop_parse_caps(bad[i],&out)); assert(out==123);
    }
    assert(!omdrop_parse_caps(NULL,&out));
    assert(!omdrop_parse_caps("0",NULL));
    puts("probe_core: capability parsing, malformed-input, and bit-boundary assertions passed (host only)");
    return 0;
}

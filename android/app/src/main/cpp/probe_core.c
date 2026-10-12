/* SPDX-License-Identifier: GPL-3.0-or-later */
#include "probe_core.h"
#include <stddef.h>
int omdrop_parse_caps(const char *hex, uint64_t *out) {
    if (!hex || !out) return 0;
    uint64_t value = 0;
    unsigned count = 0;
    while (*hex == ' ' || *hex == '\t') ++hex;
    for (; *hex && *hex != '\n' && *hex != '\r'; ++hex) {
        unsigned digit;
        if (*hex >= '0' && *hex <= '9') digit = (unsigned)(*hex - '0');
        else if (*hex >= 'a' && *hex <= 'f') digit = (unsigned)(*hex - 'a' + 10);
        else if (*hex >= 'A' && *hex <= 'F') digit = (unsigned)(*hex - 'A' + 10);
        else return 0;
        if (++count > 16) return 0;
        value = (value << 4) | digit;
    }
    if (!count) return 0;
    if (*hex == '\r') ++hex;
    if (*hex == '\n') ++hex;
    if (*hex) return 0;
    *out = value;
    return 1;
}
int omdrop_has_cap(uint64_t caps, unsigned bit) {
    return bit < 64 && (caps & (UINT64_C(1) << bit)) != 0;
}

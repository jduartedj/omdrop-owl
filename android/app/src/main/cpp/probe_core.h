/* SPDX-License-Identifier: GPL-3.0-or-later */
#ifndef OMDROP_PROBE_CORE_H
#define OMDROP_PROBE_CORE_H
#include <stdint.h>
int omdrop_parse_caps(const char *hex, uint64_t *out);
int omdrop_has_cap(uint64_t caps, unsigned bit);
#endif

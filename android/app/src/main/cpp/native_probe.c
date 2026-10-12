/* SPDX-License-Identifier: GPL-3.0-or-later */
#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include "probe_core.h"

JNIEXPORT jlongArray JNICALL
Java_org_omdrop_android_NativeProbe_readOnlyProbe(JNIEnv *env, jclass clazz) {
    (void)clazz;
    jlong values[8] = {0, 0, 0, 0, 0, 0, 0, -1};
    uint64_t caps = 0;
    FILE *status = fopen("/proc/self/status", "re");
    if (status) {
        char line[256];
        while (fgets(line, sizeof(line), status)) {
            if (strncmp(line, "CapEff:", 7) == 0) {
                values[0] = omdrop_parse_caps(line + 7, &caps);
                break;
            }
        }
        fclose(status);
    }
    if (values[0]) {
        values[1] = omdrop_has_cap(caps, 12);
        values[2] = omdrop_has_cap(caps, 13);
    }
    // Protocol zero: cannot receive any packets. Never bind/connect/send/recv/ioctl.
    int fd = socket(AF_PACKET, SOCK_RAW | SOCK_CLOEXEC, 0);
    if (fd >= 0) { values[3] = 1; close(fd); }
    else values[4] = errno;
    fd = socket(AF_NETLINK, SOCK_RAW | SOCK_CLOEXEC, NETLINK_GENERIC);
    if (fd >= 0) { values[5] = 1; close(fd); }
    else values[6] = errno;
    // Socket creation is NOT authorization for nl80211 commands.
    FILE *enforce = fopen("/sys/fs/selinux/enforce", "re");
    if (enforce) {
        int ch = fgetc(enforce);
        if (ch == '0' || ch == '1') values[7] = ch - '0';
        fclose(enforce);
    }
    jlongArray result = (*env)->NewLongArray(env, 8);
    if (result) (*env)->SetLongArrayRegion(env, result, 0, 8, values);
    return result;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <stdlib.h>
#include <stdio.h>

// This program encodes assumptions about how 128-bit values are stored in memory
// on 64-bit machines for big-endian and little-endian architectures.
// These assumptions have been checked when compiling with gcc and clang
// for ARM64, x86_64, MIPS64 (little and big endian),
// Power64 (little and big endian) and RISC-V (rv64gc).
// This can be done by cross-compiling with -O3 and checking that the compiler
// optimizes all assertions away such that the main function is basically empty.
// The easiest way to do this is to use https://godbolt.org/z/Gz7aez

static void print_bytes(unsigned char *buf, size_t size) {
    for (size_t i = 0; i < size; i++) {
        printf("%x", buf[i]);
    }
    printf("\n");
}

int main() {
    __uint128_t x = 0x7766554433221100ULL | ((__uint128_t)0xFFEEDDCCBBAA9988ULL << 64);
    __uint64_t *a = (__uint64_t *)&x;
    unsigned char *c = (unsigned char *)&x;
    // print_bytes(c, sizeof(x));

#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
    assert(c[0] == 0x00);
    assert(c[1] == 0x11);
    assert(c[2] == 0x22);
    assert(c[3] == 0x33);
    assert(c[4] == 0x44);
    assert(c[5] == 0x55);
    assert(c[6] == 0x66);
    assert(c[7] == 0x77);
    assert(c[8] == 0x88);
    assert(c[9] == 0x99);
    assert(c[10] == 0xAA);
    assert(c[11] == 0xBB);
    assert(c[12] == 0xCC);
    assert(c[13] == 0xDD);
    assert(c[14] == 0xEE);
    assert(c[15] == 0xFF);
    assert(a[0] == 0x7766554433221100ULL);
    assert(a[1] == 0xFFEEDDCCBBAA9988ULL);
#elif __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    assert(c[0] == 0xFF);
    assert(c[1] == 0xEE);
    assert(c[2] == 0xDD);
    assert(c[3] == 0xCC);
    assert(c[4] == 0xBB);
    assert(c[5] == 0xAA);
    assert(c[6] == 0x99);
    assert(c[7] == 0x88);
    assert(c[8] == 0x77);
    assert(c[9] == 0x66);
    assert(c[10] == 0x55);
    assert(c[11] == 0x44);
    assert(c[12] == 0x33);
    assert(c[13] == 0x22);
    assert(c[14] == 0x11);
    assert(c[15] == 0x00);
    assert(a[0] == 0xFFEEDDCCBBAA9988ULL);
    assert(a[1] == 0x7766554433221100ULL);
#else
    assert(0);
#endif
    return 0;
}

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
// The easiest way to do this is to use https://godbolt.org/z/fqz5en

static void print_bytes(unsigned char *buf, size_t size) {
    for (size_t i = 0; i < size; i++) {
        printf("%x", buf[i]);
    }
    printf("\n");
}

int main() {
    union test {
        __uint128_t x;
        struct {
            __uint64_t a;
            __uint64_t b;
        };
        unsigned char c[sizeof(__uint128_t)];
    } t;
    t.x = 0x7766554433221100ULL | ((__uint128_t)0xFFEEDDCCBBAA9988ULL << 64);
    // print_bytes(t.c, sizeof(t.c));

#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
    assert(t.c[0] == 0x00);
    assert(t.c[1] == 0x11);
    assert(t.c[2] == 0x22);
    assert(t.c[3] == 0x33);
    assert(t.c[4] == 0x44);
    assert(t.c[5] == 0x55);
    assert(t.c[6] == 0x66);
    assert(t.c[7] == 0x77);
    assert(t.c[8] == 0x88);
    assert(t.c[9] == 0x99);
    assert(t.c[10] == 0xAA);
    assert(t.c[11] == 0xBB);
    assert(t.c[12] == 0xCC);
    assert(t.c[13] == 0xDD);
    assert(t.c[14] == 0xEE);
    assert(t.c[15] == 0xFF);
    assert(t.a == 0x7766554433221100ULL);
    assert(t.b == 0xFFEEDDCCBBAA9988ULL);
#elif __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    assert(t.c[0] == 0xFF);
    assert(t.c[1] == 0xEE);
    assert(t.c[2] == 0xDD);
    assert(t.c[3] == 0xCC);
    assert(t.c[4] == 0xBB);
    assert(t.c[5] == 0xAA);
    assert(t.c[6] == 0x99);
    assert(t.c[7] == 0x88);
    assert(t.c[8] == 0x77);
    assert(t.c[9] == 0x66);
    assert(t.c[10] == 0x55);
    assert(t.c[11] == 0x44);
    assert(t.c[12] == 0x33);
    assert(t.c[13] == 0x22);
    assert(t.c[14] == 0x11);
    assert(t.c[15] == 0x00);
    assert(t.a == 0xFFEEDDCCBBAA9988ULL);
    assert(t.b == 0x7766554433221100ULL);
#else
    assert(0);
#endif
    return 0;
}

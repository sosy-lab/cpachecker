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

// This program encodes assumptions about how 64-bit values are stored in memory
// on 32-bit machines for big-endian and little-endian architectures.
// These assumptions have been checked when compiling with gcc and clang
// for ARM (v7, v8), x86, MIPS (little and big endian), and RISC-V (rv32gc).
// This can be done by cross-compiling with -O3 and checking that the compiler
// optimizes all assertions away such that the main function is basically empty.
// The easiest way to do this is to use https://godbolt.org/z/ecoqv3
// (for checking x86, select x86_64 and add -m32 as compiler option).

static void print_bytes(unsigned char *buf, size_t size) {
    for (size_t i = 0; i < size; i++) {
        printf("%x", buf[i]);
    }
    printf("\n");
}

int main() {
    union test {
        __uint64_t x;
        struct {
            __uint32_t a;
            __uint32_t b;
        };
        unsigned char c[sizeof(__uint64_t)];
    } t;
    t.x = 0x7766554433221100ULL;
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
    assert(t.a == 0x33221100UL);
    assert(t.b == 0x77665544UL);
#elif __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    assert(t.c[0] == 0x77);
    assert(t.c[1] == 0x66);
    assert(t.c[2] == 0x55);
    assert(t.c[3] == 0x44);
    assert(t.c[4] == 0x33);
    assert(t.c[5] == 0x22);
    assert(t.c[6] == 0x11);
    assert(t.c[7] == 0x00);
    assert(t.a == 0x77665544UL);
    assert(t.b == 0x33221100UL);
#else
    assert(0);
#endif
    return 0;
}

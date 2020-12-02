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
// The easiest way to do this is to use https://godbolt.org/z/P4ncjq
// (for checking x86, select x86_64 and add -m32 as compiler option).

static void print_bytes(unsigned char *buf, size_t size) {
    for (size_t i = 0; i < size; i++) {
        printf("%x", buf[i]);
    }
    printf("\n");
}

int main() {
    __uint64_t x = 0x7766554433221100ULL;
    __uint32_t *a = (__uint32_t *)&x;
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
    assert(a[0] == 0x33221100UL);
    assert(a[1] == 0x77665544UL);
#elif __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    assert(c[0] == 0x77);
    assert(c[1] == 0x66);
    assert(c[2] == 0x55);
    assert(c[3] == 0x44);
    assert(c[4] == 0x33);
    assert(c[5] == 0x22);
    assert(c[6] == 0x11);
    assert(c[7] == 0x00);
    assert(a[0] == 0x77665544UL);
    assert(a[1] == 0x33221100UL);
#else
    assert(0);
#endif
    return 0;
}

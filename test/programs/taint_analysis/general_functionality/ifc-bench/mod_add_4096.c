// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark was collected from http://www.cs.princeton.edu/∼aartig/benchmarks/ifc bench.zip.
// from the paper "Lazy Self-composition for Security Verification"

#include <stdlib.h>

//---------------------------------------------------------------------------//
// verifier functions                                                        //
//---------------------------------------------------------------------------//
extern void __VERIFIER_assume(int);
#define assume(X) __VERIFIER_assume(X)

extern int nd(void);

typedef unsigned int word_t;
typedef unsigned long long dword_t;

extern void  ifc_set_secret(word_t*);
extern void  ifc_check_taint(int*);
extern void  ifc_check_out(int*);

#define BIGINT_LENGTH 128 // 128 * 32 == 4096
#define WORD_MASK 0xffffffff
#define WORD_SIZE (sizeof(word_t)*8)

int bigint_extract_bit(word_t a[BIGINT_LENGTH], int bit) {
    int word_index = bit / WORD_SIZE;
    int bit_index = bit & 31;
    return (a[word_index] >> bit_index) & 0x1;
}

void bigint_set_bit(word_t a[BIGINT_LENGTH], int bit, int v) {
    int word_index = bit / WORD_SIZE;
    int bit_index = bit & 31;
    if (v == 1) {
        a[word_index] |= (1u << bit_index);
    } else {
        a[word_index] &= ~(1u << bit_index);
    }
}

void bigint_add(word_t a[BIGINT_LENGTH], word_t b[BIGINT_LENGTH], word_t sum[BIGINT_LENGTH], int *steps)
{
    int i;
    dword_t ai, bi, ci, si;

    // carry is initially zero.
    ci = 0;
    // now loop through the integers.
    for (i=0; i < BIGINT_LENGTH; i++) {
        *steps = *steps  + 1;

        ai = a[i]; 
        bi = b[i];
        si = ai + bi + ci;
        sum[i] = si & WORD_MASK;
        ci = si >> WORD_SIZE; // save the carry for next time.
    }
}


int main()
{
    int i;
    int steps = 0;

    word_t a[BIGINT_LENGTH];
    word_t b[BIGINT_LENGTH];
    word_t prod[BIGINT_LENGTH];

    for(i = 0; i < BIGINT_LENGTH; i++) {
//        word_t secret1 = nd(), secret2 = nd();
        word_t secret1 = __VERIFIER_nondet_int(), secret2 = __VERIFIER_nondet_int();
//        ifc_set_secret(&secret1);
//        ifc_set_secret(&secret2);
        __VERIFIER_set_public(&secret1, 0);
        __VERIFIER_set_public(&secret2, 0);
        a[i] = secret1;
        b[i] = secret2;
    }

    bigint_add(a, b, prod, &steps);
    __VERIFIER_is_public(&steps, 1);
//    ifc_check_taint(&steps);
//    ifc_check_out(&steps);
}


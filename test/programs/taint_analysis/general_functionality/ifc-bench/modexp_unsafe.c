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

// Should be commented out for verification.
// #define TEST

#ifdef TEST
#include <stdio.h>
#include <assert.h>
#endif

//---------------------------------------------------------------------------//
// verifier functions                                                        //
//---------------------------------------------------------------------------//
extern void __VERIFIER_assume(int);
#define assume(X) __VERIFIER_assume(X)
extern void ifc_set_secret(int c, ...);
extern void ifc_check_out(int c, ...);
extern void ifc_set_low(int c, ...);
extern int nd(void);

typedef unsigned int word_t;
typedef unsigned long long dword_t;

#define BIGINT_LENGTH 64 // 64 * 32 == 2048
#define WORD_MASK 0xffffffff
#define WORD_SIZE (sizeof(word_t)*8)

int steps = 0;

int bigint_extract_bit(word_t a[BIGINT_LENGTH], int bit) {
    int word_index = bit / WORD_SIZE;
    int bit_index = bit & 31;
#ifdef TEST
    assert (word_index < BIGINT_LENGTH);
    assert (bit_index < WORD_SIZE);
#endif
    return (a[word_index] >> bit_index) & 0x1;
}

void bigint_set_bit(word_t a[BIGINT_LENGTH], int bit, int v) {
    int word_index = bit / WORD_SIZE;
    int bit_index = bit & 31;
#ifdef TEST
    assert (word_index < BIGINT_LENGTH);
    assert (bit_index < WORD_SIZE);
#endif

    if (v == 1) {
        a[word_index] |= (1u << bit_index);
    } else {
        a[word_index] &= ~(1u << bit_index);
    }
}

void bigint_add(word_t a[BIGINT_LENGTH], word_t b[BIGINT_LENGTH], word_t sum[BIGINT_LENGTH])
{
    int i;
    dword_t ai, bi, ci, si;

    // carry is initially zero.
    ci = 0;
    // now loop through the integers.
    for (i=0; i < BIGINT_LENGTH; i++) {
        ai = a[i]; bi = b[i];
        si = ai + bi + ci;
        sum[i] = si & WORD_MASK;
        ci = si >> WORD_SIZE; // save the carry for next time.
    }
}

void bigint_shiftleft(word_t in[BIGINT_LENGTH], int shift, word_t out[BIGINT_LENGTH])
{
    int i;
    for (i = 0; i < BIGINT_LENGTH*WORD_SIZE; i++) {
        steps += 1;
        if (i < shift) {
            bigint_set_bit(out, i, 0);
        } else {
            int bi = bigint_extract_bit(in, i - shift);
            bigint_set_bit(out, i, bi);
        }
    }

}

void bigint_mul(word_t a[BIGINT_LENGTH], word_t b[BIGINT_LENGTH], word_t prod[BIGINT_LENGTH])
{
    int i;
    word_t shifted_b[BIGINT_LENGTH];
    // set prod to zero.
    for (i = 0; i < BIGINT_LENGTH; i++) { prod[i] = 0; }
    for (i = 0; i < BIGINT_LENGTH*WORD_SIZE; i++) {
        int bi = bigint_extract_bit(a, i);
        if (bi == 1) {
            bigint_shiftleft(b, i, shifted_b);
            bigint_add(prod, shifted_b, prod);
        }
    }
}

#ifndef TEST
int main()
{
    int i;
    ifc_set_low(1, BIGINT_LENGTH);
    word_t a[BIGINT_LENGTH];
    word_t b[BIGINT_LENGTH];
    word_t prod[BIGINT_LENGTH];

    // this code is a bit-ugly because we can't use a loop here.
    word_t a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63;
    word_t b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15, b16, b17, b18, b19, b20, b21, b22, b23, b24, b25, b26, b27, b28, b29, b30, b31, b32, b33, b34, b35, b36, b37, b38, b39, b40, b41, b42, b43, b44, b45, b46, b47, b48, b49, b50, b51, b52, b53, b54, b55, b56, b57, b58, b59, b60, b61, b62, b63;
    ifc_set_secret(1, a0); ifc_set_secret(1, a1); ifc_set_secret(1, a2); ifc_set_secret(1, a3); ifc_set_secret(1, a4); ifc_set_secret(1, a5); ifc_set_secret(1, a6); ifc_set_secret(1, a7); ifc_set_secret(1, a8); ifc_set_secret(1, a9); ifc_set_secret(1, a10); ifc_set_secret(1, a11); ifc_set_secret(1, a12); ifc_set_secret(1, a13); ifc_set_secret(1, a14); ifc_set_secret(1, a15); 
    ifc_set_secret(1, a16); ifc_set_secret(1, a17); ifc_set_secret(1, a18); ifc_set_secret(1, a19); ifc_set_secret(1, a20); ifc_set_secret(1, a21); ifc_set_secret(1, a22); ifc_set_secret(1, a23); ifc_set_secret(1, a24); ifc_set_secret(1, a25); ifc_set_secret(1, a26); ifc_set_secret(1, a27); ifc_set_secret(1, a28); ifc_set_secret(1, a29); ifc_set_secret(1, a30); ifc_set_secret(1, a31); 
    ifc_set_secret(1, a32); ifc_set_secret(1, a33); ifc_set_secret(1, a34); ifc_set_secret(1, a35); ifc_set_secret(1, a36); ifc_set_secret(1, a37); ifc_set_secret(1, a38); ifc_set_secret(1, a39); ifc_set_secret(1, a40); ifc_set_secret(1, a41); ifc_set_secret(1, a42); ifc_set_secret(1, a43); ifc_set_secret(1, a44); ifc_set_secret(1, a45); ifc_set_secret(1, a46); ifc_set_secret(1, a47); 
    ifc_set_secret(1, a48); ifc_set_secret(1, a49); ifc_set_secret(1, a50); ifc_set_secret(1, a51); ifc_set_secret(1, a52); ifc_set_secret(1, a53); ifc_set_secret(1, a54); ifc_set_secret(1, a55); ifc_set_secret(1, a56); ifc_set_secret(1, a57); ifc_set_secret(1, a58); ifc_set_secret(1, a59); ifc_set_secret(1, a60); ifc_set_secret(1, a61); ifc_set_secret(1, a62); ifc_set_secret(1, a63); 
    ifc_set_secret(1, b0); ifc_set_secret(1, b1); ifc_set_secret(1, b2); ifc_set_secret(1, b3); ifc_set_secret(1, b4); ifc_set_secret(1, b5); ifc_set_secret(1, b6); ifc_set_secret(1, b7); ifc_set_secret(1, b8); ifc_set_secret(1, b9); ifc_set_secret(1, b10); ifc_set_secret(1, b11); ifc_set_secret(1, b12); ifc_set_secret(1, b13); ifc_set_secret(1, b14); ifc_set_secret(1, b15); 
    ifc_set_secret(1, b16); ifc_set_secret(1, b17); ifc_set_secret(1, b18); ifc_set_secret(1, b19); ifc_set_secret(1, b20); ifc_set_secret(1, b21); ifc_set_secret(1, b22); ifc_set_secret(1, b23); ifc_set_secret(1, b24); ifc_set_secret(1, b25); ifc_set_secret(1, b26); ifc_set_secret(1, b27); ifc_set_secret(1, b28); ifc_set_secret(1, b29); ifc_set_secret(1, b30); ifc_set_secret(1, b31); 
    ifc_set_secret(1, b32); ifc_set_secret(1, b33); ifc_set_secret(1, b34); ifc_set_secret(1, b35); ifc_set_secret(1, b36); ifc_set_secret(1, b37); ifc_set_secret(1, b38); ifc_set_secret(1, b39); ifc_set_secret(1, b40); ifc_set_secret(1, b41); ifc_set_secret(1, b42); ifc_set_secret(1, b43); ifc_set_secret(1, b44); ifc_set_secret(1, b45); ifc_set_secret(1, b46); ifc_set_secret(1, b47); 
    ifc_set_secret(1, b48); ifc_set_secret(1, b49); ifc_set_secret(1, b50); ifc_set_secret(1, b51); ifc_set_secret(1, b52); ifc_set_secret(1, b53); ifc_set_secret(1, b54); ifc_set_secret(1, b55); ifc_set_secret(1, b56); ifc_set_secret(1, b57); ifc_set_secret(1, b58); ifc_set_secret(1, b59); ifc_set_secret(1, b60); ifc_set_secret(1, b61); ifc_set_secret(1, b62); ifc_set_secret(1, b63); 
    a[0] = a0; a[1] = a1; a[2] = a2; a[3] = a3; a[4] = a4; a[5] = a5; a[6] = a6; a[7] = a7; a[8] = a8; a[9] = a9; a[10] = a10; a[11] = a11; a[12] = a12; a[13] = a13; a[14] = a14; a[15] = a15;
    a[16] = a16; a[17] = a17; a[18] = a18; a[19] = a19; a[20] = a20; a[21] = a21; a[22] = a22; a[23] = a23; a[24] = a24; a[25] = a25; a[26] = a26; a[27] = a27; a[28] = a28; a[29] = a29; a[30] = a30; a[31] = a31;
    a[32] = a32; a[33] = a33; a[34] = a34; a[35] = a35; a[36] = a36; a[37] = a37; a[38] = a38; a[39] = a39; a[40] = a40; a[41] = a41; a[42] = a42; a[43] = a43; a[44] = a44; a[45] = a45; a[46] = a46; a[47] = a47;
    a[48] = a48; a[49] = a49; a[50] = a50; a[51] = a51; a[52] = a52; a[53] = a53; a[54] = a54; a[55] = a55; a[56] = a56; a[57] = a57; a[58] = a58; a[59] = a59; a[60] = a60; a[61] = a61; a[62] = a62; a[63] = a63;
    b[0] = b0; b[1] = b1; b[2] = b2; b[3] = b3; b[4] = b4; b[5] = b5; b[6] = b6; b[7] = b7; b[8] = b8; b[9] = b9; b[10] = b10; b[11] = b11; b[12] = b12; b[13] = b13; b[14] = b14; b[15] = b15;
    b[16] = b16; b[17] = b17; b[18] = b18; b[19] = b19; b[20] = b20; b[21] = b21; b[22] = b22; b[23] = b23; b[24] = b24; b[25] = b25; b[26] = b26; b[27] = b27; b[28] = b28; b[29] = b29; b[30] = b30; b[31] = b31;
    b[32] = b32; b[33] = b33; b[34] = b34; b[35] = b35; b[36] = b36; b[37] = b37; b[38] = b38; b[39] = b39; b[40] = b40; b[41] = b41; b[42] = b42; b[43] = b43; b[44] = b44; b[45] = b45; b[46] = b46; b[47] = b47;
    b[48] = b48; b[49] = b49; b[50] = b50; b[51] = b51; b[52] = b52; b[53] = b53; b[54] = b54; b[55] = b55; b[56] = b56; b[57] = b57; b[58] = b58; b[59] = b59; b[60] = b60; b[61] = b61; b[62] = b62; b[63] = b63;
    
    word_t shifted_b[BIGINT_LENGTH];
    // set prod to zero.
    for (i = 0; i < BIGINT_LENGTH; i++) { prod[i] = 0; }
    for (i = 0; i < BIGINT_LENGTH*WORD_SIZE; i++) {
        int bi = bigint_extract_bit(a, i);
        if (bi == 1) {
            bigint_shiftleft(b, i, shifted_b);
            bigint_add(prod, shifted_b, prod);
            steps = steps + BIGINT_LENGTH;
        }
    }
    ifc_check_out(1, steps);
    return 0;
}
#else
//---------------------------------------------------------------------------//
// This code is only for testing. Not to be included in the verification.    //
//---------------------------------------------------------------------------//
void bigint_read(FILE* fp, word_t a[BIGINT_LENGTH])
{
    int i;
    for (i=0; i < BIGINT_LENGTH; i++) {
        scanf("%x", a+i);
    }
}

void bigint_write(FILE* fp, word_t a[BIGINT_LENGTH])
{
    int i;
    for(i=0; i < BIGINT_LENGTH; i++) {
        printf("%x ", a[i]);
    }
}

int main(int argc, char* argv[])
{
    int i;
    int a[BIGINT_LENGTH];
    int b[BIGINT_LENGTH];
    int sum_exp[BIGINT_LENGTH];
    int mul_exp[BIGINT_LENGTH];
    int sum[BIGINT_LENGTH];
    int mul[BIGINT_LENGTH];
    int good_sum = 1, good_mul = 1;

    bigint_read(stdin, a);
    bigint_read(stdin, b);
    bigint_read(stdin, sum_exp);
    bigint_read(stdin, mul_exp);

    bigint_add(a, b, sum);
    for (i=0; i < BIGINT_LENGTH; i++) {
        if(sum[i] != sum_exp[i]) {
            good_sum = 0;
        }
    }
    bigint_write(stdout, sum);
    printf("\n%d\n", good_sum);

    bigint_mul(a, b, mul);
    for (i=0; i < BIGINT_LENGTH; i++) {
        if(mul[i] != mul_exp[i]) {
            good_mul = 0;
        }
    }
    bigint_write(stdout, mul);
    printf("\n%d\n", good_mul);

    printf("steps=%d\n", steps);

    return ((good_sum && good_mul) == 1);
}
#endif

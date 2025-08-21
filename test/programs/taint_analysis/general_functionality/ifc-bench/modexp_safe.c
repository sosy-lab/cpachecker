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
extern void ifc_check_live(int c, ...);
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
    word_t zero[BIGINT_LENGTH], shifted_zero[BIGINT_LENGTH];
    // set prod to zero, zero.
    for (i = 0; i < BIGINT_LENGTH; i++) { zero[i] = prod[i] = 0; }
    for (i = 0; i < BIGINT_LENGTH*WORD_SIZE; i++) {
        int bi = bigint_extract_bit(a, i);
        if (bi == 1) {
            bigint_shiftleft(b, i, shifted_b);
            bigint_add(prod, shifted_b, prod);
        } else {
            bigint_shiftleft(zero, i, shifted_zero);
            bigint_add(prod, shifted_zero, prod);
        }
    }
}

#ifndef TEST

int main()
{
    int i = 0;
    word_t a[BIGINT_LENGTH];
    word_t b[BIGINT_LENGTH];
    word_t prod[BIGINT_LENGTH];
    ifc_set_low(0, i);
    ifc_check_live(1, BIGINT_LENGTH);

    // this code is a bit-ugly because we can't use a loop here.
    word_t a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63;
    word_t b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15, b16, b17, b18, b19, b20, b21, b22, b23, b24, b25, b26, b27, b28, b29, b30, b31, b32, b33, b34, b35, b36, b37, b38, b39, b40, b41, b42, b43, b44, b45, b46, b47, b48, b49, b50, b51, b52, b53, b54, b55, b56, b57, b58, b59, b60, b61, b62, b63;
    __VERIFIER_set_public(a0, 0); __VERIFIER_set_public(a1, 0); __VERIFIER_set_public(a2, 0); __VERIFIER_set_public(a3, 0); __VERIFIER_set_public(a4, 0); __VERIFIER_set_public(a5, 0); __VERIFIER_set_public(a6, 0); __VERIFIER_set_public(a7, 0); __VERIFIER_set_public(a8, 0); __VERIFIER_set_public(a9, 0); __VERIFIER_set_public(a10, 0); __VERIFIER_set_public(a11, 0); __VERIFIER_set_public(a12, 0); __VERIFIER_set_public(a13, 0); __VERIFIER_set_public(a14, 0); __VERIFIER_set_public(a15, 0);
    __VERIFIER_set_public(a16, 0); __VERIFIER_set_public(a17, 0); __VERIFIER_set_public(a18, 0); __VERIFIER_set_public(a19, 0); __VERIFIER_set_public(a20, 0); __VERIFIER_set_public(a21, 0); __VERIFIER_set_public(a22, 0); __VERIFIER_set_public(a23, 0); __VERIFIER_set_public(a24, 0); __VERIFIER_set_public(a25, 0); __VERIFIER_set_public(a26, 0); __VERIFIER_set_public(a27, 0); __VERIFIER_set_public(a28, 0); __VERIFIER_set_public(a29, 0); __VERIFIER_set_public(a30, 0); __VERIFIER_set_public(a31, 0);
    __VERIFIER_set_public(a32, 0); __VERIFIER_set_public(a33, 0); __VERIFIER_set_public(a34, 0); __VERIFIER_set_public(a35, 0); __VERIFIER_set_public(a36, 0); __VERIFIER_set_public(a37, 0); __VERIFIER_set_public(a38, 0); __VERIFIER_set_public(a39, 0); __VERIFIER_set_public(a40, 0); __VERIFIER_set_public(a41, 0); __VERIFIER_set_public(a42, 0); __VERIFIER_set_public(a43, 0); __VERIFIER_set_public(a44, 0); __VERIFIER_set_public(a45, 0); __VERIFIER_set_public(a46, 0); __VERIFIER_set_public(a47, 0);
    __VERIFIER_set_public(a48, 0); __VERIFIER_set_public(a49, 0); __VERIFIER_set_public(a50, 0); __VERIFIER_set_public(a51, 0); __VERIFIER_set_public(a52, 0); __VERIFIER_set_public(a53, 0); __VERIFIER_set_public(a54, 0); __VERIFIER_set_public(a55, 0); __VERIFIER_set_public(a56, 0); __VERIFIER_set_public(a57, 0); __VERIFIER_set_public(a58, 0); __VERIFIER_set_public(a59, 0); __VERIFIER_set_public(a60, 0); __VERIFIER_set_public(a61, 0); __VERIFIER_set_public(a62, 0); __VERIFIER_set_public(a63, 0);
    __VERIFIER_set_public(b0, 0); __VERIFIER_set_public(b1, 0); __VERIFIER_set_public(b2, 0); __VERIFIER_set_public(b3, 0); __VERIFIER_set_public(b4, 0); __VERIFIER_set_public(b5, 0); __VERIFIER_set_public(b6, 0); __VERIFIER_set_public(b7, 0); __VERIFIER_set_public(b8, 0); __VERIFIER_set_public(b9, 0); __VERIFIER_set_public(b10, 0); __VERIFIER_set_public(b11, 0); __VERIFIER_set_public(b12, 0); __VERIFIER_set_public(b13, 0); __VERIFIER_set_public(b14, 0); __VERIFIER_set_public(b15, 0);
    __VERIFIER_set_public(b16, 0); __VERIFIER_set_public(b17, 0); __VERIFIER_set_public(b18, 0); __VERIFIER_set_public(b19, 0); __VERIFIER_set_public(b20, 0); __VERIFIER_set_public(b21, 0); __VERIFIER_set_public(b22, 0); __VERIFIER_set_public(b23, 0); __VERIFIER_set_public(b24, 0); __VERIFIER_set_public(b25, 0); __VERIFIER_set_public(b26, 0); __VERIFIER_set_public(b27, 0); __VERIFIER_set_public(b28, 0); __VERIFIER_set_public(b29, 0); __VERIFIER_set_public(b30, 0); __VERIFIER_set_public(b31, 0);
    __VERIFIER_set_public(b32, 0); __VERIFIER_set_public(b33, 0); __VERIFIER_set_public(b34, 0); __VERIFIER_set_public(b35, 0); __VERIFIER_set_public(b36, 0); __VERIFIER_set_public(b37, 0); __VERIFIER_set_public(b38, 0); __VERIFIER_set_public(b39, 0); __VERIFIER_set_public(b40, 0); __VERIFIER_set_public(b41, 0); __VERIFIER_set_public(b42, 0); __VERIFIER_set_public(b43, 0); __VERIFIER_set_public(b44, 0); __VERIFIER_set_public(b45, 0); __VERIFIER_set_public(b46, 0); __VERIFIER_set_public(b47, 0);
    __VERIFIER_set_public(b48, 0); __VERIFIER_set_public(b49, 0); __VERIFIER_set_public(b50, 0); __VERIFIER_set_public(b51, 0); __VERIFIER_set_public(b52, 0); __VERIFIER_set_public(b53, 0); __VERIFIER_set_public(b54, 0); __VERIFIER_set_public(b55, 0); __VERIFIER_set_public(b56, 0); __VERIFIER_set_public(b57, 0); __VERIFIER_set_public(b58, 0); __VERIFIER_set_public(b59, 0); __VERIFIER_set_public(b60, 0); __VERIFIER_set_public(b61, 0); __VERIFIER_set_public(b62, 0); __VERIFIER_set_public(b63, 0);
    a[0] = a0; a[1] = a1; a[2] = a2; a[3] = a3; a[4] = a4; a[5] = a5; a[6] = a6; a[7] = a7; a[8] = a8; a[9] = a9; a[10] = a10; a[11] = a11; a[12] = a12; a[13] = a13; a[14] = a14; a[15] = a15;
    a[16] = a16; a[17] = a17; a[18] = a18; a[19] = a19; a[20] = a20; a[21] = a21; a[22] = a22; a[23] = a23; a[24] = a24; a[25] = a25; a[26] = a26; a[27] = a27; a[28] = a28; a[29] = a29; a[30] = a30; a[31] = a31;
    a[32] = a32; a[33] = a33; a[34] = a34; a[35] = a35; a[36] = a36; a[37] = a37; a[38] = a38; a[39] = a39; a[40] = a40; a[41] = a41; a[42] = a42; a[43] = a43; a[44] = a44; a[45] = a45; a[46] = a46; a[47] = a47;
    a[48] = a48; a[49] = a49; a[50] = a50; a[51] = a51; a[52] = a52; a[53] = a53; a[54] = a54; a[55] = a55; a[56] = a56; a[57] = a57; a[58] = a58; a[59] = a59; a[60] = a60; a[61] = a61; a[62] = a62; a[63] = a63;
    b[0] = b0; b[1] = b1; b[2] = b2; b[3] = b3; b[4] = b4; b[5] = b5; b[6] = b6; b[7] = b7; b[8] = b8; b[9] = b9; b[10] = b10; b[11] = b11; b[12] = b12; b[13] = b13; b[14] = b14; b[15] = b15;
    b[16] = b16; b[17] = b17; b[18] = b18; b[19] = b19; b[20] = b20; b[21] = b21; b[22] = b22; b[23] = b23; b[24] = b24; b[25] = b25; b[26] = b26; b[27] = b27; b[28] = b28; b[29] = b29; b[30] = b30; b[31] = b31;
    b[32] = b32; b[33] = b33; b[34] = b34; b[35] = b35; b[36] = b36; b[37] = b37; b[38] = b38; b[39] = b39; b[40] = b40; b[41] = b41; b[42] = b42; b[43] = b43; b[44] = b44; b[45] = b45; b[46] = b46; b[47] = b47;
    b[48] = b48; b[49] = b49; b[50] = b50; b[51] = b51; b[52] = b52; b[53] = b53; b[54] = b54; b[55] = b55; b[56] = b56; b[57] = b57; b[58] = b58; b[59] = b59; b[60] = b60; b[61] = b61; b[62] = b62; b[63] = b63;

    bigint_mul(a, b, prod);
//    ifc_check_out(1, steps);
    __VERIFIER_is_public(steps, 1);
    return 0;
}

#else
//---------------------------------------------------------------------------//
// This code is only for testing. Not to be included in the verification.    //
//---------------------------------------------------------------------------//
//void bigint_read(FILE* fp, word_t a[BIGINT_LENGTH])
//{
//    int i;
//    for (i=0; i < BIGINT_LENGTH; i++) {
//        scanf("%x", a+i);
//    }
//}
//
//void bigint_write(FILE* fp, word_t a[BIGINT_LENGTH])
//{
//    int i;
//    for(i=0; i < BIGINT_LENGTH; i++) {
//        printf("%x ", a[i]);
//    }
//}
//
//int main(int argc, char* argv[])
//{
//    int i;
//    int a[BIGINT_LENGTH];
//    int b[BIGINT_LENGTH];
//    int sum_exp[BIGINT_LENGTH];
//    int mul_exp[BIGINT_LENGTH];
//    int sum[BIGINT_LENGTH];
//    int mul[BIGINT_LENGTH];
//    int good_sum = 1, good_mul = 1;
//
//    bigint_read(stdin, a);
//    bigint_read(stdin, b);
//    bigint_read(stdin, sum_exp);
//    bigint_read(stdin, mul_exp);
//
//    bigint_add(a, b, sum);
//    for (i=0; i < BIGINT_LENGTH; i++) {
//        if(sum[i] != sum_exp[i]) {
//            good_sum = 0;
//        }
//    }
//    bigint_write(stdout, sum);
//    printf("\n%d\n", good_sum);
//
//    bigint_mul(a, b, mul);
//    for (i=0; i < BIGINT_LENGTH; i++) {
//        if(mul[i] != mul_exp[i]) {
//            good_mul = 0;
//        }
//    }
//    bigint_write(stdout, mul);
//    printf("\n%d\n", good_mul);
//    printf("steps=%d\n", steps);
//
//    return ((good_sum && good_mul) == 1);
//}
#endif

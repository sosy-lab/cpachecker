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
extern char nd_ch(void);
extern void ifc_set_secret(int c, ...);
extern void ifc_check_out(int c, ...);
extern void ifc_set_low(int c, ...);
#define N 16

int main() {
    char pwd[N]; // secret.
    char input[N]; // public.
    int i, j;
    int bad;

    // initialize pwd.
    for(i = 0; i < N-1; i++) {
//        char pwd_ch = = (nd_ch());
        char pwd_ch = __VERIFIER_nondet_char();
//        ifc_set_secret(1, pwd_ch);
        __VERIFIER_set_public(pwd_ch, 0);
        assume(pwd_ch > 32 && pwd_ch < 127); // ascii
        pwd[i] = pwd_ch;
        if (__VERIFIER_nondet_int()) { break; }
    }
    pwd[i] = '\0';
//    ifc_set_secret(1, i);
    __VERIFIER_set_public(i, 0);

    // initialize input
    for(i = 0; i < N-1; i++) {
//        input[i] = (nd_ch());
        input[i] = __VERIFIER_nondet_char();
        ifc_set_low(1, input[i]);
        assume((int)input[i] > 32 && (int)input[i] < 127); // ascii
        if (__VERIFIER_nondet_int()) { break; }
    }
    input[i] = '\0';


    // now compare the two.
    bad = 0;
    j = 0;
    ifc_set_low(1, j);
    while (j < N && pwd[j] && input[j])
    {
        if (pwd[j] == input[j]) {j = j + 1;}
        else {bad = 1; break; }
    }

//    ifc_check_out(1, j);
    __VERIFIER_is_public(j, 0);
    return 0;
}

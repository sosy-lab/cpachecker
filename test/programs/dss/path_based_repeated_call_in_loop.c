// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Minimized from
// test/programs/benchmarks/coreutils-v9.5-units/seq_cmp_antisymmetry_cover_proof.i
// main() unconditionally reaches reach_error(), so the correct verdict is
// FALSE. With --dss --option distributedSummaries.blockAnalysisType=PATH_BASED
// CPAchecker wrongly reports TRUE.
//
// The trigger is not about arrays, malloc, or comparisons: it is that
// assume_or_exit() -- a function that may call the noreturn exit() on some
// inputs, but otherwise returns -- is called from three distinct call sites
// in main(), one of which is inside a loop that is guaranteed to execute at
// least once. The second call site's condition (on "dummy") is tautologically
// true for an unsigned char and is otherwise irrelevant to the program; it
// still suffices to trigger the bug. With only two call sites to
// assume_or_exit, or with the loop removed, or with the extra call site
// replaced by a call to a distinct (non-shared) function, CPAchecker
// correctly reports FALSE.

extern void __assert_fail(void) __attribute__((__noreturn__));
typedef long unsigned int size_t;

extern void exit(int status) __attribute__((__noreturn__));

extern unsigned char __VERIFIER_nondet_uchar();
extern size_t __VERIFIER_nondet_size_t();

void reach_error() {
    __assert_fail();
}
void assume_or_exit(int condition) {
    if (!condition) {
        exit(0);
    }
}

int main() {
    size_t num_digits_a = __VERIFIER_nondet_size_t();
    assume_or_exit(num_digits_a > 0 && num_digits_a < 100);
    unsigned char dummy = __VERIFIER_nondet_uchar();
    assume_or_exit(dummy >= 0 && dummy <= 255);
    char digit_a;
    digit_a = __VERIFIER_nondet_uchar();
    for (unsigned char i = 0; i < num_digits_a; i++) {
        unsigned char c2 = __VERIFIER_nondet_uchar();
        assume_or_exit(c2 >= '0' && c2 <= '9');
        digit_a = c2;
    }
    reach_error();
    return 0;
}

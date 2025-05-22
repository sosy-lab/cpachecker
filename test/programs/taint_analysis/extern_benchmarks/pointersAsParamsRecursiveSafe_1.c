// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int tainted = __VERIFIER_nondet_int();
    int result_test = 1;
    int result_tainted = 1;
//    int result_generic = 1;
    int result_generic2 = tainted;

    factorial(&result_test, 100);
    __VERIFIER_is_public(result_test, 1);

    factorial(&result_tainted, tainted);
    __VERIFIER_is_public(result_tainted, 0);

//    factorial(&result_generic, argc); // for us this case is covered by the previous call
//    __VERIFIER_is_public(result_test, 1);

    factorial(&result_generic2, 1000);
    __VERIFIER_is_public(result_test, 0);
}

void factorial(int *result, int n) {
// TODO: Implement proper handling of pointers
    if (n == 0)
        return;
    *result = *result * n;
    factorial (result, n - 1);
}

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
//    int x, b1, b2, y;
    int x, y;
    x = __VERIFIER_nondet_int();

    // calls to even and odd are mutual recursion and therefore not supported by our analysis
//    b1 = even(10);
//    __VERIFIER_is_public(b1, 1);
//
//    b2 = odd(x);
//    __VERIFIER_is_public(b2, 1);

    y = compute(x);
    __VERIFIER_is_public(y, 1);
}

//int even(int x) {
//    if (x == 0)
//        return 1;
//    else
//        return odd(x - 1);
//}

//int odd(int x) {
//    if (x == 1)
//        return 0;
//    else
//        return even(x - 1);
//}

int compute(int x) {
    int sum, i;

    if (x == 2)
        sum = __VERIFIER_nondet_int();
    else
        sum = 0;

    for (i = 0; i < x; ++ i) {
        sum += i;
    }

    return sum; // t(sum) = T + U = T
}

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

// TODO: The for-loop does not terminate

int main() {
    int x, b1, b2, y;
    scanf("%d", &x);
    b1 = even(x);
    b2 = odd(10);
    y = compute(x);
    return 0;

    __VERIFIER_nondet_int(b1, 1);
    __VERIFIER_nondet_int(b2, 1);
    __VERIFIER_nondet_int(y, 0);
}

int even(int x) {
    if (x == 0)
        return 1;
    else
        return odd(x - 1);
}

int odd(int x) {
    if (x == 1)
        return 0;
    else
        return even(x - 1);
}

int compute(int x) {
    int sum, i;

    if (x == 2)
        sum = __VERIFIER_nondet_int();
    else
        sum = 0;

    for (i = 0; i < x; ++ i)
        sum += i;

    return sum;
}

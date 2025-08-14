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
    int a, b, c;
    a = __VERIFIER_nondet_int();
    b = 2;
    c = 3;

    if (a) {
      while (b > 1) {
        c = b;
        b--;
      }
    }

    __VERIFIER_is_public(a, 0);
    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(c, 0);

    if (a) {
      b = 100;
    }

    c = 10;

    __VERIFIER_is_public(b, 0);
    __VERIFIER_is_public(c, 1);
}

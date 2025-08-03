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
    int b = 200;
    int a = __VERIFIER_nondet_int();

    if (b) {
        if (a) {
            a = b = 10;
            a *= b;
        } else {
            a = 500;
        }
    }

//   TODO: Investigate
    __VERIFIER_is_public(a, 0);
}

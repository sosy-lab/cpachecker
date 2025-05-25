// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int y = 2;

    while (y < 10) {
        y++;
    }

    // y is expected to be public
    __VERIFIER_is_public(y, 0);
}

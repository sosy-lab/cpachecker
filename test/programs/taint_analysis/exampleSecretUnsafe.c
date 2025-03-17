// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The variable x is not public, but the assertion says otherwise
int main() {
    int x = __VERIFIER_nondet_int();
    // assertion violation/failure expected
    __VERIFIER_is_public(x,1);
}

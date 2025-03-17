// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int x = __VERIFIER_nondet_int();
    // sanitize secret variable
    __VERIFIER_set_public(x,1);
    // assertion violation/failure expected
    __VERIFIER_is_public(x,0);
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Variable x is tainted and the assertion agrees
int main() {
    int x = __VERIFIER_nondet_int();
    __VERIFIER_is_public(x,0);
}

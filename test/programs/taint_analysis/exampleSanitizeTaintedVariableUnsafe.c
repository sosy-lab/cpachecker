// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int x = __VERIFIER_nondet_int();
    int y = 5;
    __VERIFIER_assert_taint(y,0);
    y = y + x;
    __VERIFIER_assert_taint(y,1);
    y = y - x;
    __VERIFIER_assert_taint(y,0);
}

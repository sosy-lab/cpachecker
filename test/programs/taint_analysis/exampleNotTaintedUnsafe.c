// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Variable is not tainted, but the assertion sais otherwise
int main() {
    int x = 5;
    __VERIFIER_assert_taint(x,1);
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = __VERIFIER_nondet_int();
    int y = 1;

    int z = y < 0 ? x : y;

    // Property violation expected because the tainted branch z = x is not reachable
    __VERIFIER_is_public(z, 1);

    // TODO: The analysis wrongly explores the unreachable branch
}

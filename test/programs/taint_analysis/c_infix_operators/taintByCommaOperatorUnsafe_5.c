// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);
extern void f(int var1, int var2);
extern int g(int var);

int main() {
    int x = __VERIFIER_nondet_int();
    int y = 1;
    int z = 1;

    __VERIFIER_is_public((f(g(x + z), y), y), 1);
}

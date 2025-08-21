// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct Data {
    int value;
};

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern void __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    struct Data d;
    struct Data *p;

    int x = __VERIFIER_nondet_int();
    d.value = 1;
    p = &d;

    __VERIFIER_set_public(d.value, 1);

    p->value = x;
    __VERIFIER_is_public(p->value, 1);
}

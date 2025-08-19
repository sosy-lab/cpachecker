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

// TODO: create the unsafe individual cases

int main() {
    struct Data d;
    struct Data *p;

    int x = __VERIFIER_nondet_int();
    d.value = 1;
    p = &d;

    // d.value is expected to be tainted by x and the . operator
    d.value = x;
    // TODO: this case is not being recognized (same task result for (d.value, 0) and (d.value, 1))
    __VERIFIER_is_public(d.value, 0);
    __VERIFIER_is_public(d, 0);

    // Sanitize d.value
    __VERIFIER_set_public(d.value, 1);

    // p->value is expected to be tainted by x and the -> operator
    p->value = x;
    __VERIFIER_is_public(p->value, 0);
}

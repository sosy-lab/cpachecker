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
    int y;

    if (x) {
      y = 1;
    } else {
      y = 1;
    }

    // Since both branches assign the same value to y, y does not
    // deliver any information about x. This is a false positive.
    __VERIFIER_is_public(y, 0);

    // One could address this kind of false positive by checking whether
    // the `then-` and the `else-element` of an if-element are equal
    // (contain the exact same statements). Additionally one would have
    // to check whether there is no explicit taint propagation.
}

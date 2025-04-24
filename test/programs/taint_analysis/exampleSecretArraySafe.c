// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {

    int x = __VERIFIER_nondet_int();

    int d[1];
    d[0] = x;

    __VERIFIER_is_public(d, 0);
}

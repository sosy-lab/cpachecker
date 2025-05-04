// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int a = 1;

    int d[1];
    d[0] = a; // d is public

    // Make the array not public
    __VERIFIER_set_public(d, 0);

    // must produce a property violation
    __VERIFIER_is_public(d, 1);
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error();

int main() {
    unsigned int y;
    y = -2;
    int z;
    z = y >> 1;
    if (z == 0x7FFFFFFF) {
        ERROR:
        __VERIFIER_error();
        return 1;
    }
    return 0;
}

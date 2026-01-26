// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
    int x = 10;
    //@ assert x == 10;
    int z = x * x;
    //@ assert x == 10;
    if (x != 10) {
        ERROR: return 1;
    }
    return 0;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {
    int a = 10;
    do {
        int b = __VERIFIER_nondet_int();
        if ( b == 0 ) break;
        if ( b == 1 ) a--;
    } while ( a != 10);
    if ( a == 1 ) { ERROR : ; }
    return 0;
}

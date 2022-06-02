// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error();
extern int __VERIFIER_nondet_int();

int main() {
    int i = __VERIFIER_nondet_int();
    
    int v = i;
    int w = __VERIFIER_nondet_int();
    while (i < 5) {
    
        v = v * 2;
        w = v - 1;

        if (v == i) {
            if (v < 0) {
ERROR:
                __VERIFIER_error();
            }
        }
    }
    i = i + 1;
}

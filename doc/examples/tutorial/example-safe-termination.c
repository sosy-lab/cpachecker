// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned __VERIFIER_nondet_uint();
int main() {
    int n = 1;
    int z = __VERIFIER_nondet_uint();

    while (n <= z) {
	n = n + 1;
        z = z - 1;
    }  
    return 0;
}

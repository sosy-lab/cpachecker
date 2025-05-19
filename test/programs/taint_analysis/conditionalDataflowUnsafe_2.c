// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark program from paper
// `TaintBench: Automatic real-world malware benchmarking of android taint analysis`

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = __VERIFIER_nondet_int(); // Tainted
    int y = 1;
    int z;

    int condition = (x > 0);

    if (condition) {
      z = x; // tainted
    } else {
      z = y; // not tainted
    }

    // equivalent to
    // z = condition ? x : y;

    // Property violation expected
    __VERIFIER_is_public(z, 1);
}

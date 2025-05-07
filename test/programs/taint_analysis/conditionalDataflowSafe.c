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
extern void __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = __VERIFIER_nondet_int();
    int y = 0;
    int z = 0;

    // conditional data-flow paths
    if (x > 0) {
        y = x; // Tainted data flows into y
    } else {
        z = x; // Tainted data flows into z
    }

    // At this point either y or z is tainted
    int w = y + z;

    // No property violation expected
    __VERIFIER_is_public(d, 0);
}

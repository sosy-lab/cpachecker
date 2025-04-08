// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark idea taken from the paper
// `TaintBench: Automatic real-world malware benchmarking of android taint analysis`

int main() {

    int a = __VERIFIER_nondet_int();
    int b = 0;
    int c = 0;

    // conditional data-flow paths
    if (a > 0) {
        b = a; // Tainted data flows into b
    } else {
        c = a; // Tainted data flows into c
    }

    // at this point either b or c is tainted
    int d = b + c;

    __VERIFIER_is_public(d, 0);
}

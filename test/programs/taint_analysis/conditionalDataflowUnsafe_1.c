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

    int x = __VERIFIER_nondet_int();
    int y = 0;
    int z = 0;

    int condition = (x > 0);

    // conditional data-flow paths
    if (condition) {
        y = x; // Tainted data flows into y
    } else {
        z = x; // Tainted data flows into z
    }

    // equivalent to (see taintByConditionalTernaryOperator*.c):
    // y = condition ? x : y;
    // z = condition ? x : z;

    // By uncertainty about the truth value of the condition, both branches should be explored.
    // w contains then the taint status of the union of the taint status of y and z.
    // Therefore, w is expected to be tainted
    int w = y + z;

    // Information-flow violation expected
    __VERIFIER_is_public(w, 1);
}

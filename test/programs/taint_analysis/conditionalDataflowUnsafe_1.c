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

    if (x) {
        y = x; // t(y) = t(x) = T
    } else {
        z = x; // t(z) = t(x) = T
    }

    int w = y + z; // t(w) = t(y) + t(z) = T + T = T

    __VERIFIER_is_public(w, 1);
}

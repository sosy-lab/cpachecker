// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main(int argc) {
    int a, b, c, tainted, i;
    int a1, b1, c1, i1;
    a = b = 0;
    c = tainted = __VERIFIER_nondet_int();

    for (i = 0; i < argc; ++i) {
        a++;
        __VERIFIER_is_public(a, 0);
        while (b < 500) {
            do {
                c = argc + 1;
            } while (c < argc);
        }
        b+=tainted;
        __VERIFIER_is_public(b, 0);
        __VERIFIER_is_public(i, 0);
    }

    a1 = b1 = c1 = 0;
    for (i1 = 0; i1 < argc; ++i1) {
        a1++;
        __VERIFIER_is_public(a1, 0);
        while (b1 < 500) {
            do {
                c1 = argc + 1;
            } while (c1 < argc);
        }
        b1+=2;
        __VERIFIER_is_public(b1, 0);
        __VERIFIER_is_public(i1, 0);
    }

    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(b, 1);
    __VERIFIER_is_public(c, 0);
    __VERIFIER_is_public(i, 1);

    __VERIFIER_is_public(a1, 1);
    __VERIFIER_is_public(b1, 1);
    __VERIFIER_is_public(c1, 1);

    return 0;
}


// This program is interesting, because it shows the importance of path-sensitive vs path-insensitive analysis.
// The ground truth expected in the benchmark program, was made with path-insensitivity in mind. However, a path
// sensitive analysis like ours will recognize that in both cases, the while-loop will not terminate, because the conditions
// `b < 500` and `b1 < 500` are always true, since b and b1 are never increased.
// This is actually a non-terminating program when the args variable allows the loop bodies to be explored.
// In that case, e.g., if the the program enters the first loop it would actually never reach the second loop-block.
// Our analysis is path sensitive, but for the case when the loop-condition contains a non-deterministic value
// (not defined for the program -> could be anything), the analysis will explore both branches, similar to what a path-insensitive
// analysis normally do. Although, for well defined conditions the analysis will only explore the reachable branches.

// The __VERIFIER_in_public checks at the end of the program represent the ground-truth using a path-senstive analysis.
// The __VERIFIER_in_public checks right below the statements inside the loops represent the ground-truth using a
// path-insensitive analysis and in such case would be also valid at the end of the program execution.

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

// TODO: recursion not supported

int prod = 1;
int prod2 = 1;

int main(int argc1, int argc2) {
    int tainted = __VERIFIER_nondet_int();
    int n = 100;

//    prod = argc1;
//    prod2 = argc1;
    prod = __VERIFIER_nondet_int();
    prod2 = __VERIFIER_nondet_int();

    foo(argc2);

    __VERIFIER_is_public(prod, 0);

    bar(argc2);

    __VERIFIER_is_public(prod2, 0);
}


void foo(int n) {
// TODO: after this call the analysis must clean up the local n variable
    prod *= n;
}

void bar(int n) {
    if (n == 0)
        return;
    else {
        prod2 *= n;
        // TODO: try to manually implement a (overapproximated) recursive information-flow analysis
        // 1. check whether the functionCall.function == function in which we are right now.
        // 2. if so, the program is making a recursive call.
        // 3. then we could just check whether the passed arguments involve tainted variables, and if so, just taint them.
        // 4. More complex would be to manually check what actually happens in the recursive call.
        bar(n-1);
    }
}

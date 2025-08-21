// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_nondet_char();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int a, *b, **c;
    char *chars1, *chars2 = __VERIFIER_nondet_char();
    a = 10;
    *b = a;

    // at this point b was not properly initialized with a valid address, so the assign by dereference is ignored.
    // b expected to remain untainted
    __VERIFIER_is_public(b, 1);

    // The original assing was c = b, which is not valid. It was replaced with the following:
    c = &b; // t(c) = t(&b) = t(b) = t(a) = U
    __VERIFIER_is_public(c, 1);

    // For this to work, the compiler must implicitly cast `a` (`b`) to a memory address storing a char, something like `chars1 = (char *)&a;
    // assume this would deliver a valid memory address
    chars1 = a;
    chars2 = b;

    // chars1 and chars2 are expected to be public
    __VERIFIER_is_public(chars1, 1);
    __VERIFIER_is_public(chars2, 1);


    // Extension of the benchmark (not in the original file):

    // later taint of b:
    int x = __VERIFIER_nondet_int(); // tainted
    b = &x;

    // at this point c -> b -> &x -> tainted
    __VERIFIER_is_public(*b, 0); // dereferences to x
    __VERIFIER_is_public(b, 0); // points to x
    __VERIFIER_is_public(**c, 0); // dereferences to x
    __VERIFIER_is_public(*c, 0); // equivalent to b
    __VERIFIER_is_public(c, 0); // points to b

    x = 1;
    b = &x;
    // at this point c -> b -> &x -> not tainted
    __VERIFIER_is_public(*b, 1); // dereferences to x
    __VERIFIER_is_public(b, 1); // points to x

    __VERIFIER_is_public(**c, 1); // dereferences to x
    __VERIFIER_is_public(*c, 1); // equivalent to b
    __VERIFIER_is_public(c, 1); // points to b
}

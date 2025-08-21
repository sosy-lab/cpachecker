// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

int main(int argc, char** argv)
{
    unsigned int a, b, c, d, e, f, g;
    a = b = c = d = e = f = g = __VERIFIER_nondet_int();// all T
    a = sizeof(int);     // T(a) = U
    b = sizeof(a+c);     // T(b) = T
    c = sizeof(b);       // T(c) = T
    d = sizeof(a*a);     // T(d) = U
    e = __alignof__(int);// T(e) = U
    f = __alignof__ (a); // T(f) = U
    g = __alignof__ (b); // T(g) = U
    return a + b + c + d + e + f + g; // T(main) = T
}

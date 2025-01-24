// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Compile with:
// gcc -o remainder remainder.c -lm

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

extern float remainderf (float __x, float __y) __attribute__ ((__nothrow__ ));

void reach_error() { __assert_fail("0", "remainder.c", 27, "reach_error"); }

int main() {
  float x = 3.40282347e38f;
  float y = 3.14159274f;

  if(remainderf(x, y) == -1.40962958f) {
    // -1.40962958f is the correct result here
    // We want the analysis to find this counter-example and
    // show that its feasible
    reach_error();
  }
}

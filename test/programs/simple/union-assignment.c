// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void reach_error() { __assert_fail("0", "union-assignment.c", 22, "reach_error"); }

typedef union {
  double value;
  long long bits;
} ieee_double_shape_type;

int main() {
  ieee_double_shape_type sl_u;
  sl_u.bits = 1;
  sl_u.value = 0.1;
  
  reach_error();
}

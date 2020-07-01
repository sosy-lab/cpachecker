// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdbool.h>

extern bool __VERIFIER_nondet_bool();
extern int __VERIFIER_nondet_int();
extern int __VERIFIER_nondet_int8_t();
extern void* __VERIFIER_nondet_pointer();
extern float __VERIFIER_nondet_float();
extern long __VERIFIER_nondet_long();
extern int nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();
  bool b = __VERIFIER_nondet_bool();
  void* pointer = __VERIFIER_nondet_pointer();
  int c = __VERIFIER_nondet_int8_t();
  long d = __VERIFIER_nondet_long();
  float e = __VERIFIER_nondet_float();
  int f = nondet_int();
}

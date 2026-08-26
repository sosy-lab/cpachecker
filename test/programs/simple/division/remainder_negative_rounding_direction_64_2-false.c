// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

int main() {
  int a = -5;
  int b = -3;

  // This is incorrect for C, as C remainder truncates toward zero.
  // But plain usage of SMT Integer modulo fails, as its definition is based Euclidean division 
  // (i.e. rounding towards the nearest infinity, and therefore changing signs in modulo compared to C), 
  // resulting in 1 for SMT int mod instead of -2 in C.
  // Usage of bvsmod also fails (with the same result as int modulo).
  assert(a % b == 1);

  return 0;
}

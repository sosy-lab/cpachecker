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

  // This is incorrect for C, as C division truncates toward zero.
  // But plain usage of SMT Integer division fails, as it rounds towards the nearest infinity, 
  // i.e. -inf, resulting in 2 for SMT instead of 1 for C.
  assert(a / b == 2);

  return 0;
}

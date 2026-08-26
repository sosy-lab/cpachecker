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
  int b = 3;

  // This is correct for C, as C division truncates toward zero.
  // But plain usage of SMT Integer division fails, as it rounds towards the nearest infinity, 
  // i.e. -inf, resulting in -2 instead of -1.
  assert(a / b == -1);

  return 0;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

void reach_error() { assert(0); }

extern int __VERIFIER_nondet_int(void);

int main() {
  int x = 0;
  while(__VERIFIER_nondet_int()) {
    if (x == 1) {
      // The error occurs only in the second unrolling of the loop
      goto ERROR;
    }
    x++;
  }
  return 0;
  ERROR: reach_error();
}

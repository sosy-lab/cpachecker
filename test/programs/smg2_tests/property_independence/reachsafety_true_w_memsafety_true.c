// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

void reach_error() { __assert_fail("0", "reachsafety_true_w_memsafety_true.c", 3, "reach_error"); }

// For failures, both assertion-safety and reach-safety are violated
void __VERIFIER_assert_w_assert(int cond) {
  assert(cond); // MemTrack and MemCleanup violated for failure only
  if (!(cond)) {
    ERROR: {reach_error();abort();} // MemTrack and MemCleanup violated, but unreachable
  }
  return;
}


// This program is true for ReachSafety (neither assertion violated nor error label reached)
// and is true for MemSafety, as well as MemCleanup (at each reachable program end).
// Part of a family of programs that test non-interference of the 2 property families in SMG2.
// This program is not influenced by malloc failure.
int main() {
  int a = 1;
  int b = 2;
  int * miniArray = malloc(2 * sizeof(int));
  if (miniArray == 0) {
    return 0;
  }

  *miniArray = a;
  *(miniArray + 1) = b;

  __VERIFIER_assert_w_assert(*miniArray == a);
  __VERIFIER_assert_w_assert(*(miniArray + 1) == b);

  free(miniArray);
  return 0;
}

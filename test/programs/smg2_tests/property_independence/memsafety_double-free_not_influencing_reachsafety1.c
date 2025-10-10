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

void reach_error() { __assert_fail("0", "memsafety_double-free_not_influencing_reachsafety1.c", 3, "reach_error"); }

// For failures, both assertion-safety and reach-safety are violated
void __VERIFIER_assert_w_assert(int cond, int * toFreeForFailure) {
  assert(cond); // MemTrack and MemCleanup violated for failure?
  if (!(cond)) {
    free(toFreeForFailure);
    ERROR: {reach_error();abort();}
  }
  return;
}


// This program is false for ReachSafety (assertion violated and error label reached)
// and violates MemSafety (at a distinct location), as well as MemCleanup (at each program end).
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

  __VERIFIER_assert_w_assert(*miniArray == a, miniArray);
  free(miniArray);
  free(miniArray);
  __VERIFIER_assert_w_assert(*(miniArray + 1) == b, miniArray);  // ReachSafety should return UNKNOWN here due to double free

  return 0;
}

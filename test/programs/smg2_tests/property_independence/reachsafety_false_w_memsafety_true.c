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

void reach_error() { __assert_fail("0", "reachsafety_false_w_memsafety_true.c", 3, "reach_error"); }

// For failures, both assertion-safety and reach-safety are violated
void __VERIFIER_assert_w_assert_and_free(int cond, int * toFreeForFailure) {
  free(toFreeForFailure);
  assert(cond);
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}


// This program is false for ReachSafety (assertion violated and error label reached)
// and does not violate MemSafety or MemCleanup.
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

  __VERIFIER_assert_w_assert_and_free(*(miniArray + 1) == *miniArray, miniArray);  // ReachSafety violated

  free(miniArray); // double-free if we reach this, which does not happen
  return 0;  
}

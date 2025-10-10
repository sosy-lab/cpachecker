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

void reach_error() { __assert_fail("0", "memsafety_memtrack_not_influencing_reachsafety1.c", 3, "reach_error"); }

void __VERIFIER_assert_w_assert(int cond) {
  assert(cond);
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

void foo() {
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

  return; // Violates memtrack, but ReachSafety is allowed to continue
}

void bar() {
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
  // Violates memtrack, but ReachSafety is allowed to continue
}

// This program is true for ReachSafety
// and violates MemSafety, as well as MemCleanup.
// Part of a family of programs that test non-interference of the 2 property families in SMG2.
// This program is not influenced by malloc failure.
int main() {
  foo();
  bar();
  return 0;
}

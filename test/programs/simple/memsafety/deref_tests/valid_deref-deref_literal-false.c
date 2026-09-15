// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


// Invalid deref violation (in ILP32 and LP64) on a literal 7 that has been cast to a pointer.
int main() {

  // Add some code before the violation just so that there are some nodes in the witness
  int *ptr = 0;
  ptr = malloc(sizeof(int)); // Might fail and return 0
  if (!ptr) {
    return 1;
  }
  ptr = (int *) 7;

  // This is part of a integration test for v2 violation witnesses. Please don't change it without modifying the test!
  for (int i = 0; (0, i < *ptr);) { // Invalid deref due to the pointer pointing to 7 ;D
    i++;
  }

  // Technically leaks memory, but we fail in every case before this, so it does not matter
  return 0;
}

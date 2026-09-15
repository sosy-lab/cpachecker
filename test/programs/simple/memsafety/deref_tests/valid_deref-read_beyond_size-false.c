// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


// Invalid deref violation (in ILP32 and LP64) on a non-null pointer from malloc that was incremented beyond the size of its memory.
int main() {

  // Add some code before the violation just so that there are some nodes in the witness
  int *ptr = 0;
  ptr = malloc(sizeof(int)); // Might fail and return 0
  if (!ptr) {
    return 1;
  }

  for (int i = 0; i < 1; i++) {
    ptr++;
  }

  int something = 0;
  // This is part of a integration test for v2 violation witnesses. Please don't change it without modifying the test!
  (0, something = *ptr); // Invalid, ptr is one past

  free(--ptr);

  return something;
}

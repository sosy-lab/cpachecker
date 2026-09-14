// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


// Invalid deref violation (in ILP32 and LP64) on a pointer from malloc that has been decremented after creation, thus pointing towards memory not allocated by us.
int main() {

  // Add some code before the violation just so that there are some nodes in the witness
  int *ptr = 0;
  ptr = malloc(sizeof(int)); // Might fail and return 0
  if (!ptr) {
    return 1;
  }
  ptr = ptr - 1;

  int i = 0;
  // This is part of a integration test for v2 violation witnesses. Please don't change it without modifying the test!
  for (*ptr = 0; i < 2;) { // Invalid deref due to the pointer pointing to a memory section not allocated/owned
    i++;
  }

  return i;
}

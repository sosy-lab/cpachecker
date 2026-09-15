// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


// invalid-deref violation (in ILP32 and LP64) on a non-null, but already freed pointer from malloc with a size > 0.
int main() {

  // Add some code before the violation just so that there are some nodes in the witness
  int *ptr = 0;
  ptr = malloc(sizeof(int)); // Might fail and return 0
  if (!ptr) {
    return 1;
  }

  free(ptr); // Safe

  // This is part of a integration test for v2 violation witnesses. Please don't change it without modifying the test!
  free((void *) *ptr); // Unsafe deref before free is evaluated! So its a valid-deref violation.

  return 0;
}

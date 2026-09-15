// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


// Invalid deref violation (in ILP32 and LP64) on a null pointer from malloc that may have failed with a size > 0.
int main() {

  // Add some code before the violation just so that there are some nodes in the witness
  int *ptr = 0;
  ptr = malloc(sizeof(int)); // Might fail and return 0


  // This is part of a integration test for v2 violation witnesses. Please don't change it without modifying the test!
  for (int i = 0; i < 3; *ptr = 1) { // Invalid deref on 0 for malloc fail
    i++;
  }

  free(ptr);

  return 0;
}

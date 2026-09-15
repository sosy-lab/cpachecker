// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


// Double-free violation of valid-free (in ILP32 and LP64) on a non-null pointer from malloc with a size > 0 in a comma expression.
// This program is part of a MemSafety violation witness v2 test! Don't change it without changing the witness-test as well!
int main() {

  // Add some code before the violation just so that there are some nodes in the witness
  int *ptr = 0;
  ptr = malloc(sizeof(int)); // Might fail and return 0
  if (!ptr) {
    return 1;
  }

  // Test case for MemSafety v2 violation witness target location
  (free(ptr), /* Safe */ free(ptr) /* Unsafe, double-free! */);

  return 0;
}

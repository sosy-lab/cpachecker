// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


// Invalid deref violation (in ILP32 and LP64) on a incremented null pointer from malloc with a size > 0 that may have failed.
int main() {

  // Unsigned char has no trap representation
  unsigned char *ptr = 0;
  ptr = malloc(sizeof(char)); // Might fail and return 0

  int res = 0;

  // This is part of a integration test for v2 violation witnesses. Please don't change it without modifying the test!
  for (int i = 0; i < 1; i++) {
    res = *(++ptr); // Invalid deref for malloc failure
  }

  free(--ptr);

  return res;
}

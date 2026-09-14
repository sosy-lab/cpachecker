// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


unsigned char * p = 0;

int foo() {
  // unsigned char has no trap representation!
  p = malloc(sizeof(unsigned char)); // Might fail and return 0

  // This is part of a integration test for v2 violation witnesses. Please don't change it without modifying the test!
  return *p; // Fails for 0
}


// Invalid deref violation (in ILP32 and LP64) on a null pointer from malloc with a size > 0 that may have failed.
int main() {

  foo();
  if (p) {
    free(p);
  }

  return 0;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>
#include <alloca.h>


int * some_function(int size_of_alloca) {
  int * some_ptr = 0;
  {
    some_ptr = alloca(size_of_alloca); // Never returns 0! Must never be freed!
  }
  // The pointer is still valid here, as its scope is function bound
  return some_ptr;
}

void do_nothing1(void) {
  // We do nothing here on purpose
}


void do_nothing2(void) {
  // We do nothing here on purpose
}


// Violation of valid-free (in ILP32 and LP64) due to freeing the pointer with an address towards a local variable. 
// This program is part of a memsafety violation witness v2 test! Don't change it without changing the witness-test as well!
int main() {

  // Add some code before the violation (the alloca stuff) just so that there are some nodes in the witness
  int *ptr = 0;
  ptr = some_function(sizeof(int));
  // Scope of alloca() ended, so it is already cleaned up.

  // Test case for MemSafety v2 violation witness target location
  (do_nothing1(), do_nothing2(), free(&ptr)); // Violation. But not due to the alloca pointer, which is cleaned up properly already, but due to freeing the address of the local variable!

  return 0;
}

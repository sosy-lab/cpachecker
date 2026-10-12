// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


int fooPlus(int a, int b) {
  return a + b;
}


int fooMul(int a, int b) {
  return a * b;
}


int bar() {
  return 123;
}


int revBar() {
  return 321;
}


// This program tests basic function pointer usage with explicit and implicit & to establish correct usage of function pointers.
// It is unsafe for ReachSafety/MemSafety/Memcleanup in ILP32 and LP64.
int main() {
  int* dummyMemory = malloc(sizeof(int)); // Only used to cause a memory leak

  // Function pointers without arguments
  int (*barFunctionPtrNoAddressOp)() = bar;

  int (*barFunctionPtrWithAddressOp)();
  barFunctionPtrWithAddressOp = &revBar;

  // Function pointers with simple arguments
  int (*fooFunctionPtrNoAddressOp)(int,int);
  fooFunctionPtrNoAddressOp = fooPlus;
  int (*fooFunctionPtrWithAddressOp)(int,int) = &fooMul;

  if (barFunctionPtrNoAddressOp() != 123 || barFunctionPtrWithAddressOp() != 321 || fooFunctionPtrNoAddressOp(1, 3) != 4 || fooFunctionPtrWithAddressOp(1, 3) != 3) {
    free(dummyMemory);
    return 0; // Safe if reached
  }

  ERROR: // Expected to be reached only of all above fail
  return 1;
}

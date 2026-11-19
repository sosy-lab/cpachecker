// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


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
// It is safe for AssertionSafety/memSafety in ILP32 and LP64.
int main() {

  // Function pointers without arguments
  int (*barFunctionPtrNoAddressOp)() = bar;
  assert(barFunctionPtrNoAddressOp() == 123);

  int (*barFunctionPtrWithAddressOp)();
  barFunctionPtrWithAddressOp = &revBar;
  assert(barFunctionPtrWithAddressOp() == 321);
  assert(barFunctionPtrNoAddressOp() == 123); // barFunctionPtrNoAddressOp is still working and pointing to the correct function


  // Function pointers with simple arguments
  int (*fooFunctionPtrNoAddressOp)(int,int);
  fooFunctionPtrNoAddressOp = fooPlus;
  int (*fooFunctionPtrWithAddressOp)(int,int) = &fooMul;

  assert(fooFunctionPtrNoAddressOp(1, 3) == 4);
  assert(fooFunctionPtrWithAddressOp(1, 3) == 3);

  return 0;
}

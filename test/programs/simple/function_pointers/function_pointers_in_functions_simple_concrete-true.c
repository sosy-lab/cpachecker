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


// Returns the pointer to function fooPlus() for selector being 1, function pointer fooMul() for selector being 0, and NULL else
int (*returnFunctionDependingOnInput(int selector))(int, int) {
    if (selector == 1) {
        return &fooPlus;
    } else if (selector == 0) {
        return fooMul;
    } else {
        return 0;
    }
}


// Returns result of the given function with the 2 arguments applied in the same order as they are given here
int useTwoArgFunctionPointerGiven(int arg1, int arg2, int (*func)(int, int)) {
  return func(arg1, arg2);
}


// Returns result of the function chosen with funSelector being applied on returnFunctionDependingOnInput() with the 2 arguments applied in the same order as they are given here
int getAndUseTwoArgFunctionPointerGiven(int arg1, int arg2, int funSelector) {
  return returnFunctionDependingOnInput(funSelector)(arg1, arg2);
}


// Returns result of the given function
int useNoArgFunctionPointerGiven(int (*func)()) {
  return func();
}


// This program tests basic function pointer usage with explicit and implicit &,
// as well as the pointers being used in functions and returned from functions to establish correct usage of function pointers.
// It is safe for AssertionSafety/MemSafety in ILP32 and LP64.
int main() {

  // Start with testing giving function pointers to functions and executing the function behind the pointer in a called function
  // Function pointers without arguments
  int (*barFunctionPtrNoAddressOp)() = bar;
  assert(useNoArgFunctionPointerGiven(barFunctionPtrNoAddressOp) == 123);

  int (*barFunctionPtrWithAddressOp)();
  barFunctionPtrWithAddressOp = &revBar;
  assert(useNoArgFunctionPointerGiven(barFunctionPtrWithAddressOp) == 321);

  assert(barFunctionPtrNoAddressOp() == 123); // barFunctionPtrNoAddressOp is still working and pointing to the correct function
  assert(barFunctionPtrWithAddressOp() == 321); // barFunctionPtrWithAddressOp is still working and pointing to the correct function


  // Function pointers with simple arguments
  int (*fooFunctionPtrNoAddressOp)(int,int);
  fooFunctionPtrNoAddressOp = fooPlus;
  int (*fooFunctionPtrWithAddressOp)(int,int) = &fooMul;

  assert(useTwoArgFunctionPointerGiven(1, 3, fooFunctionPtrNoAddressOp) == 4);
  assert(useTwoArgFunctionPointerGiven(1, 3, fooFunctionPtrWithAddressOp) == 3);

  assert(fooFunctionPtrNoAddressOp(1, 3) == 4); // fooFunctionPtrNoAddressOp is still working and pointing to the correct function
  assert(fooFunctionPtrWithAddressOp(1, 3) == 3); // fooFunctionPtrWithAddressOp is still working and pointing to the correct function


  // Test creation and return of function pointers in methods.
  // We override the existing ones, but in reverse!
  fooFunctionPtrNoAddressOp = returnFunctionDependingOnInput(0); // fooFunctionPtrNoAddressOp is assigned fooMult()!
  assert(fooFunctionPtrNoAddressOp(1, 3) == 3);
  fooFunctionPtrWithAddressOp = returnFunctionDependingOnInput(1); // fooFunctionPtrWithAddressOp is assigned fooPlus()!
  assert(fooFunctionPtrWithAddressOp(1, 3) == 4);

  assert(useTwoArgFunctionPointerGiven(1, 3, fooFunctionPtrNoAddressOp) == 3);
  assert(useTwoArgFunctionPointerGiven(1, 3, fooFunctionPtrWithAddressOp) == 4);


  // Test creation and execution of function pointers in methods.
  assert(getAndUseTwoArgFunctionPointerGiven(2, 4, 0) == 8); // fooMult(2, 4)
  assert(getAndUseTwoArgFunctionPointerGiven(3, 6, 1) == 9); // fooPlus(3, 6)

  // useTwoArgFunctionPointerGiven(1, 1, fooFunctionPtrNoAddressOp) = 1 -> fooPlus(2,4) = 6
  assert(getAndUseTwoArgFunctionPointerGiven(2, 4, useTwoArgFunctionPointerGiven(1, 1, fooFunctionPtrNoAddressOp)) == 6);

  // useTwoArgFunctionPointerGiven(0, 0, fooFunctionPtrWithAddressOp) = 0 -> fooMult(3,6) = 18
  assert(getAndUseTwoArgFunctionPointerGiven(3, 6, useTwoArgFunctionPointerGiven(0, 0, fooFunctionPtrWithAddressOp)) == 18);

  return 0;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdint.h>
#include <assert.h>

// Safe for reach- and memsafety
// This tests addresses cast to numbers, manipulated, and compared and/or cast to pointers and compared.
// Does not go out-of-bounds except for +1 object of the original memory.
// No operation overflows in this program!
int main() {
  // Sanity check that we use the correct byte sizes (int pointer in 64 bits is 8 bytes)
  assert(sizeof(int *) == 8);

  int memory_size = 10;
  int *ptr = malloc(memory_size*sizeof(int));
  int *otherPtr = malloc(memory_size*sizeof(int));

  if (ptr == 0) {
    return 0;
  }
  if (otherPtr == 0) {
    return 0;
  }

  // Ensure 32 bit size of int
  assert(sizeof(int) == 4);

  intptr_t intPtr = (intptr_t) ptr;
  uintptr_t uintPtr = (uintptr_t) ptr;
  long long llPtr = (long long) ptr;
  unsigned long ulPtr = (unsigned long) ptr;

  for (int i = 0; i < memory_size; i++) {
    assert((int *) intPtr == ptr || (int *) uintPtr == ptr || (int *) llPtr == ptr || (int *) ulPtr == ptr);
    assert(intPtr == (intptr_t) ptr || uintPtr == (uintptr_t) ptr || llPtr == (long long) ptr || ulPtr == (unsigned long) ptr);

    *(int *) intPtr = i;
    *otherPtr = i + 1;

    ptr++;
    otherPtr++;
    intPtr = intPtr + 4;
    uintPtr = uintPtr + 4;
    llPtr = llPtr + 4;
    ulPtr = ulPtr + 4;
  }

  for (int i = memory_size - 1; i >= 0; i--) {
    assert(ptr == ptr);

    assert(otherPtr == otherPtr);

    // Check that the cast values are consistent before decrements
    assert((int *) intPtr == ptr || (int *) uintPtr == ptr || (int *) llPtr == ptr || (int *) ulPtr == ptr);
    assert(intPtr == (intptr_t) ptr || uintPtr == (uintptr_t) ptr || llPtr == (long long) ptr || ulPtr == (unsigned long) ptr);

    // TODO: find out whether this can be negative and add appropriate tests
    // intptr_t intPtr_before_dec = intPtr;
    uintptr_t uintPtr_before_dec = uintPtr;
    long long llPtr_before_dec = llPtr;
    unsigned long ulPtr_before_dec = ulPtr;

    ptr--;
    otherPtr--;
    intPtr = intPtr - 4;
    uintPtr = uintPtr - 4;
    llPtr = llPtr - 4;
    ulPtr = ulPtr - 4;

    // Check that the cast values are consistent after decrements
    assert(intPtr == (intptr_t) ptr || uintPtr == (uintptr_t) ptr || llPtr == (long long) ptr || ulPtr == (unsigned long) ptr);

    assert((int *) intPtr == ptr || (int *) uintPtr == ptr ||  (int *) llPtr == ptr || (int *) ulPtr == ptr);
    assert(*(int *) intPtr == i || *(int *) uintPtr == i || *(int *) llPtr == i || *(int *) ulPtr == i);

    // Distance to itself
    assert(intPtr - intPtr == 0 || uintPtr - uintPtr == 0 || llPtr - llPtr == 0 || ulPtr - ulPtr == 0);
    // Distance to before dec
    assert(uintPtr_before_dec - uintPtr == 4 || llPtr - llPtr_before_dec == -4 || ulPtr - ulPtr_before_dec == -4);

    // intptr_t can be negative (i think), but long long can fit the entire pointer in its positive range, hence they are NEVER negative!
    assert(uintPtr_before_dec > uintPtr || ulPtr_before_dec > ulPtr || llPtr_before_dec > llPtr);

    assert(uintPtr_before_dec >= uintPtr || ulPtr_before_dec >= ulPtr || llPtr_before_dec >= llPtr);

    assert( !(uintPtr_before_dec < uintPtr) || !(ulPtr_before_dec < ulPtr) || !(llPtr_before_dec < llPtr));

    assert(!(uintPtr_before_dec <= uintPtr) || !(ulPtr_before_dec <= ulPtr) || !(llPtr_before_dec <= llPtr));

    assert(uintPtr_before_dec != uintPtr || ulPtr_before_dec != ulPtr || llPtr_before_dec != llPtr);

    assert(!(uintPtr + 1 < uintPtr) || !(ulPtr + 1 < ulPtr) || !(llPtr + 1 < llPtr));

    assert(uintPtr + 1 >= uintPtr || ulPtr + 1 >= ulPtr || llPtr + 1 >= llPtr);

    assert(uintPtr + 1 > uintPtr || ulPtr + 1 > ulPtr || llPtr + 1 > llPtr);

    assert(intPtr <= intPtr || uintPtr <= uintPtr || llPtr <= llPtr || ulPtr <= ulPtr);

    assert(intPtr >= (intptr_t) ptr || uintPtr >= (uintptr_t) ptr || llPtr >= (long long) ptr || ulPtr >= (unsigned long) ptr);

    assert(!(intPtr != (intptr_t) ptr) || !(uintPtr != (uintptr_t) ptr) || !(llPtr != (long long) ptr) || !(ulPtr != (unsigned long) ptr));
  }

  int * ptrPlusOne = 1 + ptr;
  int * ptrPlusNine = ptr + 9;
  int * ptrPlusTen = ptr + 10; // Defined and valid according to C11 standard §6.5.6.8 

  // Transform to unsigned long, increment by hand (size is depending on 32/64 bit!) and back
  ulPtr = (unsigned long) ptr;

  assert((ulPtr + 4) == (unsigned long) ptrPlusOne);

  assert((ulPtr + 1) != (unsigned long) ptrPlusOne);

  assert((ulPtr + 1) != (unsigned long) ptr);
  assert((ulPtr - 1) != (unsigned long) ptr);

  assert((ulPtr + 2) != (unsigned long) ptrPlusOne);

  assert((ulPtr + 2) != (unsigned long) ptr);
  assert((ulPtr - 2) != (unsigned long) ptr);

  assert((ulPtr + 3) != (unsigned long) ptrPlusOne);

  assert((ulPtr + 3) != (unsigned long) ptr);
  assert((ulPtr - 3) != (unsigned long) ptr);


  int * ptrPlusOneIntButByHand = (int *) (ulPtr + 4);
  assert(ptrPlusOneIntButByHand == ptrPlusOne);

  int * ptrPlusOneByteButByHand = (int *) (ulPtr + 1);
  assert(ptrPlusOneByteButByHand != ptr);
  assert(ptrPlusOneByteButByHand != ptrPlusOne);
  assert(ptrPlusOneByteButByHand != (ptr + 2));
  assert(ptrPlusOneByteButByHand != (ptr + 3));


  int * ptrPlusTwoByteButByHand = (int *) (ulPtr + 2);
  assert(ptrPlusTwoByteButByHand != ptr);
  assert(ptrPlusTwoByteButByHand != ptrPlusOne);
  assert(ptrPlusTwoByteButByHand != (ptr + 2));
  assert(ptrPlusTwoByteButByHand != (ptr + 3));

  int * ptrPlusThreeByteButByHand = (int *) (ulPtr + 3);
  assert(ptrPlusThreeByteButByHand != ptr);
  assert(ptrPlusThreeByteButByHand != ptrPlusOne);
  assert(ptrPlusThreeByteButByHand != (ptr + 2));
  assert(ptrPlusThreeByteButByHand != (ptr + 3));


  int * ptrMinusOneByteButByHand = (int *) (ulPtr - 1);
  assert(ptrMinusOneByteButByHand != ptr);
  assert(ptrMinusOneByteButByHand != ptrPlusOne);
  assert(ptrMinusOneByteButByHand != (ptr + 2));
  assert(ptrMinusOneByteButByHand != (ptr + 3));

  assert(ulPtr != 0); // We know this is impossible due to ptr not being null
  
  assert((ulPtr + 4) != 1); // impossible to reach as ptr can not be 0

  assert((ulPtr + 4) != 4); // impossible to reach as ptr can not be 0 (ptr can theoretically start 1 byte in, so at "4", so the minimum value of ulPtr + 4 is 8)

  // Test the same with types guaranteed to hold all (void) pointers
  intPtr = (intptr_t) ptr;

  assert((intPtr + 4) == (intptr_t) ptrPlusOne);
  assert((int *)(intPtr + 4) == ptrPlusOne);

  assert((intPtr + (9*4)) == (intptr_t) ptrPlusNine);
  assert((int *)(intPtr + (9*4)) == ptrPlusNine);

  assert((intPtr + (10*4)) == (intptr_t) ptrPlusTen);
  assert((int *)(intPtr + (10*4)) == ptrPlusTen);

  uintPtr = (uintptr_t) ptr;

  assert((uintPtr + 4) == (uintptr_t) ptrPlusOne);
  assert((int *)(uintPtr + 4) == ptrPlusOne);

  assert((uintPtr + (9*4)) == (uintptr_t) ptrPlusNine);
  assert((int *)(uintPtr + (9*4)) == ptrPlusNine);

  assert((uintPtr + (10*4)) == (uintptr_t) ptrPlusTen);
  assert((int *)(uintPtr + (10*4)) == ptrPlusTen);

  assert((int *)(uintPtr + 8) == (int *)(intPtr + 8));


  // Check some pointers (generated out of the 2 originals) for value consistency
  assert(*(int *)(uintPtr + 8) == *(int *)(intPtr + 16) - 2 || *(int *)(uintPtr + 8) == 2 || *(int *)(uintPtr + 8) == *otherPtr + 1 || *(int *)(ulPtr + 4) == 1);
  

  free((int *)uintPtr); // initial pointer, so this is safe
  free((int *)(unsigned long)otherPtr);
  return 0; // Also memory unsafe due to not being freed in case of assertion failure
}

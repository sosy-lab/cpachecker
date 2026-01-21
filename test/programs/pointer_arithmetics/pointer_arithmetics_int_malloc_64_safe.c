// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdint.h>

// Safe for reach- and memsafety
// This tests address comparison using ==, !=, as well as <, >, <=, >=, and pointer arithmetics.
// Notes:
// == and != is basically valid on all memory addresses (pointers).
// +- is valid only inside of the memory allocated and at max 1 positive increment beyond (otherwise undef. beh.)!
// <, >, <=, >= is only valid for valid addresses (resulting from pointer arithmetics or allocation/addressOf operator &) and null.
int main() {
  int memory_size = 10;
  int *ptr = malloc(memory_size*sizeof(int));
  int *otherPtr = malloc(memory_size*sizeof(int));

  if (ptr == 0) {
    return 0;
  }
  if (otherPtr == 0) {
    return 0;
  }

  for (int i = 0; i < memory_size; i++) {
    *ptr = i;
    ptr++;
    *otherPtr = i + 1;
    otherPtr++;
  }

  for (int i = 0; i < memory_size; i++) {
    if (ptr != ptr) {
      goto ERROR;
    }

    if (otherPtr != otherPtr) {
      goto ERROR;
    }

    // Decrement first to not generate a invalid deref!
    ptr--;
    otherPtr--;

    // Check that the values are consistent after inc/dec
    if (*ptr != *ptr) {
      goto ERROR;
    }

    if (*otherPtr != *otherPtr) {
      goto ERROR;
    }

    if (*ptr + 1 != *otherPtr) {
      goto ERROR;
    }
  }


  if ((1 + ptr) != (ptr + 1)) {
    goto ERROR;
  }
  if ((ptr + 1) == ptr) {
    goto ERROR;
  }
  if ((1 + ptr) == ptr) {
    goto ERROR;
  }

  int * ptrPlusOne = 1 + ptr;
  // int * ptrMinusOne = ptr - 1; // Would be undef. beh.!
  int * ptrPlusNine = ptr + 9;
  int * ptrPlusTen = ptr + 10; // Defined and valid according to C11 standard §6.5.6.8 

  // None of the ptr originating pointers that have been inc/dec are equal to any other than themselves
  if ((ptr + 1) != ptrPlusOne) {
    goto ERROR;
  }
  if ((ptr + 9) != ptrPlusNine) {
    goto ERROR;
  }
  if ((ptr + 10) != ptrPlusTen) {
    goto ERROR;
  }

  if (ptr == ptrPlusOne) {
    goto ERROR;
  }
  if (ptr == ptrPlusNine) {
    goto ERROR;
  }
  if (ptr == ptrPlusTen) {
    goto ERROR;
  }

  if (ptrPlusOne == ptrPlusNine) {
    goto ERROR;
  }
  if (ptrPlusOne == ptrPlusTen) {
    goto ERROR;
  }

  if (ptrPlusNine == ptrPlusTen) {
    goto ERROR;
  }


  // None is equal to the other pointer
  if (otherPtr == ptr) {
    goto ERROR;
  }
  if (otherPtr == ptrPlusOne) {
    goto ERROR;
  }
  if (otherPtr == ptrPlusNine) {
    goto ERROR;
  }
  
  // Note: the check below MIGHT actually succeed if they are (by luck) directly adjacent in memory!
  // if (otherPtr == ptrPlusTen) ...
  // We test this in another test program!


  // Equality to others with calculations
  if ((ptrPlusOne - 1) != ptr) {
    goto ERROR;
  }
  if (((ptrPlusOne + 1) - 1) != ptrPlusOne) {
    goto ERROR;
  }
  if ((ptrPlusOne + 8) != ptrPlusNine) {
    goto ERROR;
  }
  if ((ptrPlusOne + 9) != ptrPlusTen) {
    goto ERROR;
  }

  if ((ptrPlusNine - 9) != ptr) {
    goto ERROR;
  }
  if (((ptrPlusNine + 9) - 9) != ptrPlusNine) {
    goto ERROR;
  }
  if ((ptrPlusNine - 8) != ptrPlusOne) {
    goto ERROR;
  }
  if ((ptrPlusNine + 1) != ptrPlusTen) {
    goto ERROR;
  }

  if ((ptrPlusTen - 10) != ptr) {
    goto ERROR;
  }
  if (((ptrPlusTen - 10) + 10) != ptrPlusTen) {
    goto ERROR;
  }
  if ((ptrPlusTen - 1) != ptrPlusNine) {
    goto ERROR;
  }
  if ((ptrPlusTen - 9) != ptrPlusOne) {
    goto ERROR;
  }


  // More than 1 calculation
  if (((ptr + 3) - 4) != (ptrPlusOne - 2)) {
    goto ERROR;
  }
  if (((ptr + 11) - 4) != (ptrPlusNine - 2)) {
    goto ERROR;
  }
  if (((ptr + 13) - 5) != (ptrPlusTen - 2)) {
    goto ERROR;
  }


  // < > <= >= operators

  // < false
  if (otherPtr < otherPtr) {
    goto ERROR;
  }
  if (((ptr + 6) - 3) < (ptrPlusOne + 2)) {
    goto ERROR;
  }
  if (((ptr + 11) - 4) < (ptrPlusNine - 2)) {
    goto ERROR;
  }
  if (((ptr + 8) - 2) < (ptrPlusTen - 4)) {
    goto ERROR;
  }

  // < true (check as negated)
  if (!(((ptr + 6) - 4) < (ptrPlusOne + 2))) { // 0 + 6 - 4 = 2 < 3 = 1 + 2
    goto ERROR;
  }
  if (!(((ptr + 11) - 5) < (ptrPlusNine - 2))) { // 11 - 5 = 6 < 7 = 9 - 2
    goto ERROR;
  }
  if (!(((ptr + 8) - 3) < (ptrPlusTen - 4))) { // 8 - 3 = 5 < 6 = 10 - 4
    goto ERROR;
  }

  // > false
  if (otherPtr > otherPtr) {
    goto ERROR;
  }
  if (((ptr + 6) - 3) > (ptrPlusOne + 2)) {
    goto ERROR;
  }
  if (((ptr + 11) - 4) > (ptrPlusNine - 2)) {
    goto ERROR;
  }
  if (((ptr + 8) - 2) > (ptrPlusTen - 4)) {
    goto ERROR;
  }

  // > true (checked as negated)
  if (!(((ptr + 6) - 2) > (ptrPlusOne + 2))) { // 0 + 6 - 2 = 4 > 3 = 1 + 2
    goto ERROR;
  }
  if (!(((ptr + 11) - 3) > (ptrPlusNine - 2))) { // 11 - 3 = 8 > 7 = 9 - 2
    goto ERROR;
  }
  if (!(((ptr + 8) - 1) > (ptrPlusTen - 4))) { // 8 - 1 = 7 > 6 = 10 - 4
    goto ERROR;
  }

  // <= false
  if (((ptr + 6) - 2) <= (ptrPlusOne + 2)) {
    goto ERROR;
  }
  if (((ptr + 11) - 3) <= (ptrPlusNine - 2)) {
    goto ERROR;
  }
  if (((ptr + 8) - 1) <= (ptrPlusTen - 4)) {
    goto ERROR;
  }


  // <= true
  if (!(otherPtr <= otherPtr)) {
    goto ERROR;
  }
  if (!(((ptr + 6) - 3) <= (ptrPlusOne + 2))) { // 0 + 6 - 3 = 3 = 3 = 1 + 2
    goto ERROR;
  }
  if (!(((ptr + 11) - 4) <= (ptrPlusNine - 2))) { // 11 - 4 = 7 = 7 = 9 - 2
    goto ERROR;
  }
  if (!(((ptr + 8) - 2) <= (ptrPlusTen - 4))) { // 8 - 2 = 6 = 6 = 10 - 4
    goto ERROR;
  }

  if (!(((ptr + 6) - 4) <= (ptrPlusOne + 2))) { // 0 + 6 - 4 = 2 <= 3 = 1 + 2
    goto ERROR;
  }
  if (!(((ptr + 11) - 5) <= (ptrPlusNine - 2))) { // 11 - 5 = 6 <= 7 = 9 - 2
    goto ERROR;
  }
  if (!(((ptr + 8) - 3) <= (ptrPlusTen - 4))) { // 8 - 3 = 5 <= 6 = 10 - 4
    goto ERROR;
  }

  // >= false
  if (((ptr + 6) - 4) >= (ptrPlusOne + 2)) {
    goto ERROR;
  }
  if (((ptr + 11) - 5) >= (ptrPlusNine - 2)) {
    goto ERROR;
  }
  if (((ptr + 8) - 3) >= (ptrPlusTen - 4)) {
    goto ERROR;
  }


  // >= true
  if (!((otherPtr) >= (otherPtr))) {
    goto ERROR;
  }
  if (!(((ptr + 6) - 3) >= (ptrPlusOne + 2))) { // 0 + 6 - 3 = 3 >= 3 = 1 + 2
    goto ERROR;
  }
  if (!(((ptr + 11) - 4) >= (ptrPlusNine - 2))) { // 11 - 4 = 7 >= 7 = 9 - 2
    goto ERROR;
  }
  if (!(((ptr + 8) - 2) >= (ptrPlusTen - 4))) { // 8 - 2 = 6 >= 6 = 10 - 4
    goto ERROR;
  }

  if (!(((ptr + 6) - 2) >= (ptrPlusOne + 2))) { // 0 + 6 - 2 = 4 >= 3 = 1 + 2
    goto ERROR;
  }
  if (!(((ptr + 11) - 3) >= (ptrPlusNine - 2))) { // 11 - 3 = 8 >= 7 = 9 - 2
    goto ERROR;
  }
  if (!(((ptr + 8) - 1) >= (ptrPlusTen - 4))) { // 8 - 1 = 7 >= 6 = 10 - 4
    goto ERROR;
  }

  // Distance
  if (ptr - ptr != 0) {
    goto ERROR;
  }

  if (ptr - ptrPlusOne != -1) {
    goto ERROR;
  }
  if (ptrPlusOne - ptr != 1) {
    goto ERROR;
  }

  if (ptr - ptrPlusNine != -9) {
    goto ERROR;
  }
  if (ptrPlusNine - ptr != 9) {
    goto ERROR;
  }

  if (ptr - ptrPlusTen != -10) {
    goto ERROR;
  }
  if (ptrPlusTen - ptr != 10) {
    goto ERROR;
  }

  if ((ptr + 1) - (ptrPlusTen - 1) != -8) {
    goto ERROR;
  }
  if ((ptrPlusTen - 1) - (ptr + 1) != 8) {
    goto ERROR;
  }


  // Transform to unsigned long, increment by hand (size is depending on 32/64 bit!) and back
  unsigned long long ullPtr = (unsigned long long) ptr;
  // Sanity check that we use the correct byte sizes (pointers in 64bit are 8 bytes)
  if (sizeof(ptr) != 8) {
    goto ERROR;
  }
  // Integer is 4 bytes
  if (sizeof(int) != 4) {
    goto ERROR;
  }

  if ((ullPtr + 4) != (unsigned long long) ptrPlusOne) {
    goto ERROR;
  }

  if ((ullPtr + 1) == (unsigned long long) ptrPlusOne) {
    goto ERROR;
  }

  if ((ullPtr + 1) == (unsigned long long) ptr) {
    goto ERROR;
  }
  if ((ullPtr - 1) == (unsigned long long) ptr) {
    goto ERROR;
  }

  if ((ullPtr + 2) == (unsigned long long) ptrPlusOne) {
    goto ERROR;
  }

  if ((ullPtr + 2) == (unsigned long long) ptr) {
    goto ERROR;
  }
  if ((ullPtr - 2) == (unsigned long long) ptr) {
    goto ERROR;
  }

  if ((ullPtr + 3) == (unsigned long long) ptrPlusOne) {
    goto ERROR;
  }

  if ((ullPtr + 3) == (unsigned long long) ptr) {
    goto ERROR;
  }
  if ((ullPtr - 3) == (unsigned long long) ptr) {
    goto ERROR;
  }


  int * ptrPlusOneIntButByHand = (int *) (ullPtr + 4);
  if (ptrPlusOneIntButByHand != ptrPlusOne) {
    goto ERROR;
  }

  int * ptrPlusOneByteButByHand = (int *) (ullPtr + 1);
  if (ptrPlusOneByteButByHand == ptr) {
    goto ERROR;
  }
  if (ptrPlusOneByteButByHand == ptrPlusOne) {
    goto ERROR;
  }
  if (ptrPlusOneByteButByHand == (ptr + 2)) {
    goto ERROR;
  }
  if (ptrPlusOneByteButByHand == (ptr + 3)) {
    goto ERROR;
  }


  int * ptrPlusTwoByteButByHand = (int *) (ullPtr + 2);
  if (ptrPlusTwoByteButByHand == ptr) {
    goto ERROR;
  }
  if (ptrPlusTwoByteButByHand == ptrPlusOne) {
    goto ERROR;
  }
  if (ptrPlusTwoByteButByHand == (ptr + 2)) {
    goto ERROR;
  }
  if (ptrPlusTwoByteButByHand == (ptr + 3)) {
    goto ERROR;
  }

  int * ptrPlusThreeByteButByHand = (int *) (ullPtr + 3);
  if (ptrPlusThreeByteButByHand == ptr) {
    goto ERROR;
  }
  if (ptrPlusThreeByteButByHand == ptrPlusOne) {
    goto ERROR;
  }
  if (ptrPlusThreeByteButByHand == (ptr + 2)) {
    goto ERROR;
  }
  if (ptrPlusThreeByteButByHand == (ptr + 3)) {
    goto ERROR;
  }


  int * ptrMinusOneByteButByHand = (int *) (ullPtr - 1);
  if (ptrMinusOneByteButByHand == ptr) {
    goto ERROR;
  }
  if (ptrMinusOneByteButByHand == ptrPlusOne) {
    goto ERROR;
  }
  if (ptrMinusOneByteButByHand == (ptr + 2)) {
    goto ERROR;
  }
  if (ptrMinusOneByteButByHand == (ptr + 3)) {
    goto ERROR;
  }

  if (ullPtr == 0) {
    goto ERROR; // We know this is impossible due to ptr not being null
  }

  if ((ullPtr + 4) == 1) {
    goto ERROR; // impossible to reach as ptr can not be 0
  }

  if ((ullPtr + 4) == 4) {
    goto ERROR; // impossible to reach as ptr can not be 0 (ptr can theoretically start 1 byte in, so at "4", so the minimum value of ulPtr + 4 is 8)
  }

 
  // Test the same with types guaranteed to hold all (void) pointers
  intptr_t intPtr = (intptr_t) ptr;

  if ((intPtr + 4) != (intptr_t) ptrPlusOne) {
    goto ERROR;
  }
  if ((int *)(intPtr + 4) != ptrPlusOne) {
    goto ERROR;
  }

  if ((intPtr + (9*4)) != (intptr_t) ptrPlusNine) {
    goto ERROR;
  }
  if ((int *)(intPtr + (9*4)) != ptrPlusNine) {
    goto ERROR;
  }

  if ((intPtr + (10*4)) != (intptr_t) ptrPlusTen) {
    goto ERROR;
  }
  if ((int *)(intPtr + (10*4)) != ptrPlusTen) {
    goto ERROR;
  }

  uintptr_t uintPtr = (uintptr_t) ptr;

  if ((uintPtr + 4) != (uintptr_t) ptrPlusOne) {
    goto ERROR;
  }
  if ((int *)(uintPtr + 4) != ptrPlusOne) {
    goto ERROR;
  }

  if ((uintPtr + (9*4)) != (uintptr_t) ptrPlusNine) {
    goto ERROR;
  }
  if ((int *)(uintPtr + (9*4)) != ptrPlusNine) {
    goto ERROR;
  }

  if ((uintPtr + (10*4)) != (uintptr_t) ptrPlusTen) {
    goto ERROR;
  }
  if ((int *)(uintPtr + (10*4)) != ptrPlusTen) {
    goto ERROR;
  }

  if ((int *)(uintPtr + 8) != (int *)(intPtr + 8)) {
    goto ERROR;
  }


  // Check some pointers (generated out of the 2 originals) for value consistency
  if (*(int *)(uintPtr + 16) != *(int *)(intPtr + 16) || *(int *)(uintPtr + 16) != 4 || *(int *)(uintPtr + 16) != *otherPtr + 3 || *(int *)(ullPtr + 8) != 2) {
    goto ERROR;
  }

  free(ptr);
  free(otherPtr);
  return 0;

  ERROR:
  return 1; // Also memory unsafe due to not being freed, but unreachable
}

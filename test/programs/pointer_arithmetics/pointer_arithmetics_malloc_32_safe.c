// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <stdlib.h>

// Safe for reach- and memsafety
int main() {
  int *ptr = malloc(10*sizeof(int));
  int *otherPtr = malloc(10*sizeof(int));

  if (ptr == 0) {
    return 0;
  }
  if (otherPtr == 0) {
    return 0;
  }

  if (ptr != ptr) {
    goto ERROR;
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
  int * ptrMinusOne = ptr - 1;
  int * ptrPlusNine = ptr + 9;
  int * ptrPlusTen = ptr + 10;

  // None of the ptr originating pointers that have been inc/dec are equal to any other than themselves
  if ((ptr + 1) != ptrPlusOne) {
    goto ERROR;
  }
  if ((ptr - 1) != ptrMinusOne) {
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
  if (ptr == ptrMinusOne) {
    goto ERROR;
  }
  if (ptr == ptrPlusNine) {
    goto ERROR;
  }
  if (ptr == ptrPlusTen) {
    goto ERROR;
  }

  if (ptrPlusOne == ptrMinusOne) {
    goto ERROR;
  }
  if (ptrPlusOne == ptrPlusNine) {
    goto ERROR;
  }
  if (ptrPlusOne == ptrPlusTen) {
    goto ERROR;
  }

  if (ptrMinusOne == ptrPlusNine) {
    goto ERROR;
  }
  if (ptrMinusOne == ptrPlusTen) {
    goto ERROR;
  }

  if (ptrPlusNine == ptrPlusTen) {
    goto ERROR;
  }
  if (ptrMinusOne == ptrPlusTen) {
    goto ERROR;
  }


  // None is equal to the other pointer
  if (otherPtr == ptr) {
    goto ERROR;
  }
  if (otherPtr == ptrPlusOne) {
    goto ERROR;
  }
  if (otherPtr == ptrMinusOne) {
    goto ERROR;
  }
  if (otherPtr == ptrPlusNine) {
    goto ERROR;
  }
  if (otherPtr == ptrPlusTen) {
    goto ERROR;
  }

  if ((otherPtr - 1) == ptr) {
    goto ERROR;
  }
  if ((otherPtr - 1) == ptrPlusOne) {
    goto ERROR;
  }
  if ((otherPtr - 1) == ptrMinusOne) {
    goto ERROR;
  }
  if ((otherPtr - 1) == ptrPlusNine) {
    goto ERROR;
  }
  if ((otherPtr - 1) == ptrPlusTen) {
    goto ERROR;
  }

  if ((otherPtr + 10) == ptr) {
    goto ERROR;
  }
  if ((otherPtr + 10) == ptrPlusOne) {
    goto ERROR;
  }
  if ((otherPtr + 10) == ptrMinusOne) {
    goto ERROR;
  }
  if ((otherPtr + 10) == ptrPlusNine) {
    goto ERROR;
  }
  if ((otherPtr + 10) == ptrPlusTen) {
    goto ERROR;
  }


  // Equality to others with calculations
  if ((ptrPlusOne - 1) != ptr) {
    goto ERROR;
  }
  if (((ptrPlusOne + 1) - 1) != ptrPlusOne) {
    goto ERROR;
  }
  if ((ptrPlusOne - 2) != ptrMinusOne) {
    goto ERROR;
  }
  if ((ptrPlusOne + 8) != ptrPlusNine) {
    goto ERROR;
  }
  if ((ptrPlusOne + 9) != ptrPlusTen) {
    goto ERROR;
  }

  if ((ptrMinusOne + 1) != ptr) {
    goto ERROR;
  }
  if (((ptrMinusOne + 1) - 1) != ptrMinusOne) {
    goto ERROR;
  }
  if ((ptrMinusOne + 2) != ptrPlusOne) {
    goto ERROR;
  }
  if ((ptrMinusOne + 10) != ptrPlusNine) {
    goto ERROR;
  }
  if ((ptrMinusOne + 11) != ptrPlusTen) {
    goto ERROR;
  }

  if ((ptrPlusNine - 9) != ptr) {
    goto ERROR;
  }
  if (((ptrPlusNine + 9) - 9) != ptrPlusNine) {
    goto ERROR;
  }
  if ((ptrPlusNine - 10) != ptrMinusOne) {
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
  if ((ptrPlusTen - 11) != ptrMinusOne) {
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
  if (((ptr + 3) - 6) != (ptrMinusOne - 2)) {
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
  if (((ptr + 5) - 3) < (ptrMinusOne + 3)) {
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
  if (!(((ptr + 5) - 4) < (ptrMinusOne + 3))) { // 0 + 5 - 4 = 1 < 2 = -1 + 3
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
  if (((ptr + 5) - 3) > (ptrMinusOne + 3)) {
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
  if (!(((ptr + 5) - 2) > (ptrMinusOne + 3))) { // 0 + 5 - 2 = 3 > 2 = -1 + 3
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
  if (((ptr + 5) - 2) <= (ptrMinusOne + 3)) {
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
  if (!(((ptr + 5) - 3) <= (ptrMinusOne + 3))) { // 0 + 5 - 3 = 2 = 2 = -1 + 3
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
  if (!(((ptr + 5) - 4) <= (ptrMinusOne + 3))) { // 0 + 5 - 4 = 1 <= 2 = -1 + 3
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
  if (((ptr + 5) - 4) >= (ptrMinusOne + 3)) {
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
  if (!(((ptr + 5) - 3) >= (ptrMinusOne + 3))) { // 0 + 5 - 3 = 2 >= 2 = -1 + 3
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
  if (!(((ptr + 5) - 2) >= (ptrMinusOne + 3))) { // 0 + 5 - 2 = 3 >= 2 = -1 + 3
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

  if (ptr - ptrMinusOne != 1) {
    goto ERROR;
  }
  if (ptrMinusOne - ptr != -1) {
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

  if ((ullPtr + 4) != (unsigned long long) ptrPlusOne) {
    goto ERROR;
  }
  if ((ullPtr - 4) != (unsigned long long) ptrMinusOne) {
    goto ERROR;
  }

  if ((ullPtr + 1) == (unsigned long long) ptrPlusOne) {
    goto ERROR;
  }
  if ((ullPtr - 1) == (unsigned long long) ptrMinusOne) {
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
  if ((ullPtr - 2) == (unsigned long long) ptrMinusOne) {
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
  if ((ullPtr - 3) == (unsigned long long) ptrMinusOne) {
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
  if (((int *)(((unsigned long long) ptrPlusOneIntButByHand) - 8)) != ptrMinusOne) {
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
  if (ptrPlusOneByteButByHand == ptrMinusOne) {
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
  if (ptrPlusTwoByteButByHand == ptrMinusOne) {
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
  if (ptrPlusThreeByteButByHand == ptrMinusOne) {
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
  if (ptrMinusOneByteButByHand == ptrMinusOne) {
    goto ERROR;
  }
  

  free(ptr);
  free(otherPtr);
  return 0;

  ERROR:
  return 1; // Also memory unsafe due to not being freed, but unreachable
}

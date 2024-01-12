// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

int search_string(const char *aString) {
  int not_zero = 1;
  int num = 0;
  while (not_zero) {
    if (*aString == 0) {
      not_zero = 0;
    } else if (*aString == 'A') {

      return num;
    }
    num++;
    aString++;
  }
  return -1;
}

int search_string_max(char aString[], int max) {
  printf("search_string_max: %c\n", *aString);
  aString[0] = '.';
  for (int i = 0; i < max; i++) {
    if (aString[i] == 0) {
      return -1;
    } else if (aString[i] == 'A') {
            printf("search_string_max: %c\n", aString[i]);
      return i;
      
    }
  }
  return -1;
}

int main() {
  int cap_a_loc = search_string("hello i am a string with A big a."); // string size 34
  const char * carArray = "hello i am also a string with A big a."; // string size 39
  cap_a_loc = search_string(carArray);
  char varSizeCharArray[] = "hello i am another a string with A big a."; // string size 42
  cap_a_loc = search_string(varSizeCharArray);
  assert(cap_a_loc == search_string_max(varSizeCharArray, 42));
  char * pointerFromPrevArray = varSizeCharArray; // pointerFromPrevArray string size 42
  printf("main pointerFromPrevArray: %c\n", *pointerFromPrevArray);
  pointerFromPrevArray++; // pointerFromPrevArray string size 41 from the start
  printf("main pointerFromPrevArray: %c\n", *pointerFromPrevArray);
  int one_less = search_string_max(pointerFromPrevArray, 41);
  printf("main pointerFromPrevArray: %c\n", *pointerFromPrevArray);
  assert(one_less + 1 == cap_a_loc);
  assert('.' == *pointerFromPrevArray);
  assert('.' == varSizeCharArray[1]);

  char emptyString[32]; // Can not be used in search_string safely!
  cap_a_loc = search_string_max(emptyString, 32);
  assert(cap_a_loc == -1);

  return 0;  // SAFE for MemSafety, MemCleanup and ReachSafety
}

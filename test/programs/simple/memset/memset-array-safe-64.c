// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *memset(void *s, int c, unsigned long n);

// Dereferences a null pointer if the condition does not hold.
void check(int condition) {
  if (!condition) {
    int *null = 0;
    *null = 0;
  }
}

unsigned char globalArray[8];

// Machine model: LP64. Expected: true for valid-memsafety.
// memset writes exactly the bytes of the arrays.
int main(void) {
  unsigned long longArray[4] = { 1UL, 2UL, 3UL, 4UL };
  memset(longArray, 0, 32UL);
  check(longArray[0] == 0UL);
  check(longArray[3] == 0UL);

  unsigned char charArray[32];
  memset(charArray, 1, 32UL);
  check(charArray[0] == 1U);
  check(charArray[31] == 1U);

  memset(globalArray, 255, 8UL);
  check(globalArray[7] == 255U);

  return 0;
}

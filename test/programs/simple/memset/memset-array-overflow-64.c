// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *memset(void *s, int c, unsigned long n);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  unsigned long longArray[4];

  // Invalid: longArray has 32 bytes, memset writes 33.
  memset(longArray, 0, 33UL);

  return 0;
}

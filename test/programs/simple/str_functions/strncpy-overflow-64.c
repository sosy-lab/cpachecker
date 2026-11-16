// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern char *strncpy(char *dest, const char *src, unsigned long n);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  char dest[4];

  // Invalid: strncpy pads with zeros up to eight bytes, dest has four.
  strncpy(dest, "ab", 8UL);

  return 0;
}

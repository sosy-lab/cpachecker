// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int snprintf(char *str, unsigned long size, const char *format, ...);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  char dest[4];

  // Invalid: size 8 allows writing "abcdef" and its terminator (7 bytes), dest has four.
  snprintf(dest, 8UL, "%s", "abcdef");

  return 0;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern char *strcpy(char *dest, const char *src);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  char source[3] = {'a', 'b', 'c'};
  char dest[8];

  // Invalid: source has no terminator, so strcpy reads beyond its three bytes.
  strcpy(dest, source);

  return 0;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
extern int strcmp(const char *s1, const char *s2);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  char s[2] = {'a', 'b'};

  // Invalid: s has no terminator, so strcmp reads beyond its two bytes.
  int result = strcmp("abc", s);

  return result;
}

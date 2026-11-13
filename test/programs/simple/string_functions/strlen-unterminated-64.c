// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned long strlen(const char *s);

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
int main(void) {
  char s[3] = {'a', 'b', 'c'};

  // Invalid: s has no terminator, so strlen reads beyond its three bytes.
  unsigned long length = strlen(s);

  return (int)length;
}

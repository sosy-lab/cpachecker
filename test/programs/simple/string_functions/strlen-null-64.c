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
  char *null = 0;

  // Invalid: s is a null pointer.
  unsigned long length = strlen(null);
  (void)length;

  return 0;
}

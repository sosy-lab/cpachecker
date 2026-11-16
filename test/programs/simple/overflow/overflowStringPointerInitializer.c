// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A pointer initialized with a string literal points to the literal:
// it is not null, and the characters of the literal are not written to other objects.
char c = 'z';
char *p = "abc";
char *arr[] = {"de"};

int main(void) {
  char *q = "fg";
  int x = 2147483647;
  if (p == 0 || arr[0] == 0 || q == 0 || c != 'z') {
    x++;
  }
  return 0;
}

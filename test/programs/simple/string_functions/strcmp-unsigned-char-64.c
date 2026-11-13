// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
extern void abort(void);
void reach_error(void) { abort(); }
extern int strcmp(const char *s1, const char *s2);

// Machine model: LP64. Expected: true for unreach-call.
// strcmp compares the characters as unsigned char.
int main(void) {
  char high[2] = {(char)0xff, '\0'};

  if (strcmp(high, "a") <= 0 || strcmp("a", high) >= 0) {
    reach_error();
  }
  return 0;
}

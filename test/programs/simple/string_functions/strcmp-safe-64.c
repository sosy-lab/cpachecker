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

struct S {
  int i;
  char s[4];
};

char global[] = "xy";

// Machine model: LP64. Expected: true for unreach-call and valid-memsafety.
// strcmp compares arrays, string literals and pointers.
int main(void) {
  char a[] = "-";
  char b[] = "-";
  char c[] = "+";
  char *p = a;
  struct S st = {0, "ab"};
  char m[2][3] = {"ab", "cd"};

  if (strcmp(b, a) != 0 || strcmp(c, a) == 0) {
    reach_error();
  }
  if (strcmp(a, "-") != 0 || strcmp("-", a) != 0 || strcmp("-", "-") != 0) {
    reach_error();
  }
  if (strcmp(p, b) != 0 || strcmp(global, "xy") != 0 || strcmp(st.s, "ab") != 0) {
    reach_error();
  }

  // The first different character determines the sign, a prefix is smaller.
  if (strcmp(st.s, "ac") >= 0 || strcmp("ac", st.s) <= 0 || strcmp(m[0], m[1]) >= 0) {
    reach_error();
  }
  if (strcmp(st.s, "a") <= 0 || strcmp("a", st.s) >= 0) {
    reach_error();
  }

  // Pointers into arrays
  if (strcmp(b + 1, "") != 0 || strcmp(&st.s[1], "b") != 0 || strcmp(&m[1][1], "d") != 0) {
    reach_error();
  }
  return 0;
}

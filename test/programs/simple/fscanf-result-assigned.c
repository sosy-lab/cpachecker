// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef struct _IO_FILE FILE;
extern FILE *stdin;
extern int fscanf(FILE *stream, const char *format, ...);
extern void __assert_fail(const char *, const char *, unsigned int, const char *);
void reach_error() { __assert_fail("0", "fscanf-result-assigned.c", 3, "reach_error"); }

int main() {
  int x = 0;
  int n = fscanf(stdin, "%d", &x);
  if (n == 1 && x == 5) {
    reach_error();
  }
  return 0;
}

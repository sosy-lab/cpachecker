// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
void reach_error(void) { abort(); }
extern char *strcpy(char *dest, const char *src);
extern int strcmp(const char *s1, const char *s2);

char str[5] = "test";

int main(void) {
  char buf[10];
  char *r = strcpy(buf, str);
  buf[1] = 'x';
  if (r != buf || strcmp(buf, str) != 0) {
    reach_error();
  }
  return 0;
}

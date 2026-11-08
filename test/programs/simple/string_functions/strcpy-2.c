// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
void reach_error(void) { abort(); }
extern char *strcpy(char *, const char *);

int main(void) {
  char src[3] = {'a', 'b', 0};
  char dst[3] = {0, 0, 0};
  strcpy(dst, src);
  if (dst[1] == 'b') {
    reach_error();
  }
  return 0;
}

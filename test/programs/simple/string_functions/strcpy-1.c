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
extern int strcmp(const char *, const char *);
extern unsigned long strlen(const char *);
extern void *malloc(unsigned long);

int main(void) {
  char src[5] = {'t', 'e', 's', 't', 0};
  char dst[10];
  dst[6] = 'x';
  char *r = strcpy(dst, src);
  if (r != &dst[0] || strcmp(dst, src) != 0 || dst[4] != 0 || dst[6] != 'x') {
    reach_error();
  }
  char *p = malloc(strlen(dst) + 1);
  if (p) {
    strcpy(p, dst);
    if (p[3] != 't' || p[4] != 0) {
      reach_error();
    }
  }
  return 0;
}

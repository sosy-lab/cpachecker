// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// `p` points only to `x`, so the write through `p` does not change the address in `r`.

extern int pthread_create(unsigned long *t, void *attr, void *(*start)(void *), void *arg);
int x, z, w;
int *r = &z;
int **rr = &r;
int *s = &w;
int *p = &x;

int five() { return 5; }

void *t(void *arg) {
  int v = *r;
  *p = five();
  return 0;
}

int main() {
  unsigned long th;
  pthread_create(&th, 0, t, 0);
  w = 2;
  return 0;
}

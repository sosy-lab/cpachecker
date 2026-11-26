// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The index `x` of the access to `a` changes, but `a` and `b` do not overlap.

extern int pthread_create(unsigned long *t, void *attr, void *(*start)(void *), void *arg);
int a[10];
int b[10];
int x;

int five() { return 5; }

void *t(void *arg) {
  a[x] = 1;
  x = five();
  return 0;
}

int main() {
  unsigned long th;
  pthread_create(&th, 0, t, 0);
  b[5] = 2;
  return 0;
}

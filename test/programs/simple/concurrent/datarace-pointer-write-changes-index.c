// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The write through `p` changes the index `x` of an earlier access to `a`.

extern int pthread_create(unsigned long *t, void *attr, void *(*start)(void *), void *arg);
int a[10];
int x;
int *p = &x;

void *t(void *arg) {
  a[x] = 1;
  *p = 5;
  return 0;
}

int main() {
  unsigned long th;
  pthread_create(&th, 0, t, 0);
  a[5] = 2;
  return 0;
}

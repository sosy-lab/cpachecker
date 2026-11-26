// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// `p` gets the thread argument `&x`, so the write through `p` races with the write of `x`.

extern int pthread_create(unsigned long *t, void *attr, void *(*start)(void *), void *arg);
int x, y;
int *p = &y;

void *t(void *arg) {
  p = arg;
  *p = 1;
  return 0;
}

int main() {
  unsigned long th;
  pthread_create(&th, 0, t, &x);
  x = 2;
  return 0;
}

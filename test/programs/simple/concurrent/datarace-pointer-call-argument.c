// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The local `x` of `main` is read as call argument while the thread writes it through `g`.

extern int pthread_create(unsigned long *t, void *attr, void *(*start)(void *), void *arg);
int *g;

int id(int v) { return v; }

void *t(void *arg) {
  *g = 1;
  return 0;
}

int main() {
  int x = 0;
  g = &x;
  unsigned long th;
  pthread_create(&th, 0, t, 0);
  id(x);
  return 0;
}

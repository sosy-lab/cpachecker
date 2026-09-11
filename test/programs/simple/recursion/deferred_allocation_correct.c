// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();
extern void *calloc(unsigned long nmemb, unsigned long size);

struct s {
  int a;
};

// The allocation has no known type yet, so it is deferred until p is used as a struct s*. Every
// frame allocates its own memory, so leaving a frame must only stop tracking the pointer of that
// frame and not the one of the caller, which is still used after the recursive call returns.
void f(int n) {
  void *p = calloc(1, 4);
  if (p == 0) {
    return;
  }
  if (n > 0) {
    f(n - 1);
  }
  struct s *q = p;
  if (q->a != 0) {
ERROR:
    reach_error();
  }
}

int main() {
  f(1);
  return 0;
}

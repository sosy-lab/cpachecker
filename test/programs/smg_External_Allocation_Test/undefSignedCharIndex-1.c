// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct Recursive {
  signed char sc;
  struct Recursive *p;
  long q;
};


int foo() {
  struct Recursive *a;
  struct Recursive ar[128];
  a = malloc(sizeof(struct Recursive));
  if (a->sc >= 0) {
    a->p = ar[a->sc];
  }
  free(a);
  return 0;
}

int main() {
  foo();
  return 0;
}


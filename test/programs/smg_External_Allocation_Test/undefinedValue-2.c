// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct Recursive {
  int s;
  struct Recursive *p;
  long q;
};


int foo() {
  struct Recursive *a;
  struct Recursive *b;
  a = ext_allocation();
  a->p->p->p = malloc(sizeof(struct Recursive));
  b = a->p->p;
  if (b->s < 100) {
    free(b->p);
  }
  free(a->p->p->p);
  return 0;
}

int main() {
  foo();
  return 0;
}


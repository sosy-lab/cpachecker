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
  a = malloc(sizeof(struct Recursive));
  switch (a->q) {
    case 5:
    case 10:
      a->s = 5;
      break;
    default:
      a->p->q = 5;
      free(a);
  }
  free(a);
  return 0;
}

int main() {
  foo();
  return 0;
}


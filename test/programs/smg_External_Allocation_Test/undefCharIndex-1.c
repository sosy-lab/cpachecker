// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct Recursive {
  unsigned char uc;
  struct Recursive *p;
  long q;
};


int foo() {
  struct Recursive *a;
  struct Recursive ar[255];
  a = malloc(sizeof(struct Recursive));
  a->p = ar[a->uc];
  free(a);
  return 0;
}

int main() {
  foo();
  return 0;
}


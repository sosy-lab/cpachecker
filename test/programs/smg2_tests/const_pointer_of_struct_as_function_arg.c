// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>

struct Foo {
  int temp;
  int *p;
};

int bar(struct Foo *pa, int i) {
  short temp_as_short = (short)(*(pa+(unsigned long)i)).temp;
  assert(temp_as_short == 1);
  int value = blub(*pa);
  return value;
}

int blub(struct Foo pap) {
  struct Foo f = pap;
  return *(f.p);
}

int main() {
  int i = 0;
  int a = 0;
  struct Foo f;
  f.temp = 1;
  f.p = &a;
  struct Foo * fooPtr = &f;

  int bla;
  bla = (*fooPtr).temp;
  assert(bla == 1);

  int res = bar(fooPtr, i);

  assert(res == a); // SAFE
  return 0;
}

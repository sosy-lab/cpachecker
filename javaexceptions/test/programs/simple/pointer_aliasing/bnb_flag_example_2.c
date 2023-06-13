// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
void __VERIFIER_assume(int);

struct foo {
  int flag;
  int data;
};

void *not_calloc(size_t, size_t);

void main(void) {
  int *q;
  int x = 10;
  struct foo *p = not_calloc(1, sizeof(struct foo));
  __VERIFIER_assume(p!=0);
  p->flag = 1;
  q = &p->data;
  *q = x;
  if(p->flag != 1) {
    ERROR: goto ERROR;
  }
}


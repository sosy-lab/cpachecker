// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdio.h>
//typedef unsigned int size_t;
//extern  __attribute__((__nothrow__)) void *malloc(size_t __size )  __attribute__((__malloc__)) ;

struct node {
    short int      next;
    short int      value;
};


int main() {
  struct node f;
  struct node* tmp;
  f.value = 0;
  f.next = 0;
  tmp = &f;
  tmp->value = 1;
  tmp->next = 0;
  f = *tmp;
  if (f.value == 1) {
    goto ERROR;
  }
  printf ("SAFE\n");
  return 0;

ERROR:
  printf ("UNSAFE\n");
ERROR2:
  goto ERROR2;

  return 1;
}

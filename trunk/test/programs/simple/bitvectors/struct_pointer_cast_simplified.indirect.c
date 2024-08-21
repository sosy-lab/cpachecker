// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdio.h>

struct node {
  void* ptr;
};


int main() {
  struct node f;
  struct node g;
  struct node *tmp;
  tmp = &f;
  f.ptr = &g;
  struct node* generatedVar = (struct node*)tmp->ptr;
  generatedVar->ptr = 0;
  
  if (g.ptr != 0) {
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

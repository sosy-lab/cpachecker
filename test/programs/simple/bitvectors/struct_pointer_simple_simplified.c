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
  int* ptr;
};

int main() {
  struct node f;
  struct node* tmp;
  int t = 3;
  int* ptr = &t;
  f.ptr = ptr;
  tmp = &f;
  int* generatedVar = tmp->ptr;
  if (*generatedVar != 3) {
    goto ERROR2;
  }
  printf ("SAFE\n");
  return 0;

ERROR2:
  printf ("UNSAFE\n");
ERROR:
  goto ERROR;

  return 1;
}

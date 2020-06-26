// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdio.h>

struct stub {
    void* ptr;
    int   value;
};


int main() {
  struct stub top;
  struct stub inner;
  struct stub* tmp;
  tmp = &inner;
  top.ptr = tmp;
  
  ((struct stub*)(top.ptr))->value = 2;

  if (inner.value != 2) {
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

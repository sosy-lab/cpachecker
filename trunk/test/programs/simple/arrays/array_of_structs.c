// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>

struct test {
  int* ptr;
};

int main() {
  struct test* t[4];
  t[0] = 0;
  int p = 2;
  struct test f;
  t[1] = &f;
  (t[1])->ptr = &p;
  t[2] = 0;
  t[3] = 0;
  struct test* b = t[1]; 
  if (*(b->ptr) != 2) {
    printf ("UNSAFE\n");
    goto ERROR;
  }
  printf ("SAFE\n");

  //if(plus_one < minus_one) {
  //  goto ERROR;
  //}
  
  return (0);
  ERROR:
  return (-1);
}


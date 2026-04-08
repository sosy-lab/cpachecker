// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// #include<stdio.h>

typedef union {
  int a;
  char b[2];
} ut;

int main(){
  ut value = { .a = 5 };
  ut * vp = &value;
  value = (ut) 2;

  //printf("%d\n", vp->a); // 2\n

  if (vp->a != 2){
    goto error;
  }
  return 0;
error:
  return -1;
}

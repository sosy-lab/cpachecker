// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>

int main() {
  int size = 3;
  struct s {
    int i;
    char a1[size];
    char a2[];  // flexible array member; aka a variable length array. This does NOT have any memory associated!
  } s;

  char *p = (char*)&(s.a2[0]); // The & and [0] erase each other, so this is essentially just the address of s + the offset. (memsafety not violated)
  char *q = (char*)&s;
  printf("p = %lu != %lu = q\n", ((unsigned long)p), ((unsigned long)q));
  printf("p = %lu != %lu = q\n", ((unsigned long)p), ((unsigned long)q) + size + sizeof(int));
  if (p != q + size + sizeof(int)) {
    printf("no error");
    return 0;
  }
  printf("Error\n");
ERROR:
  return 1;
}

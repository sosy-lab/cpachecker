// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>

int main() {
  char t = 50;
  char* z1 = &t;
  void* z2 = z1;
  unsigned int* pi = z2;
  printf ("pi = %u\n", *pi);
  // We could dereference anything here
  // But the lowest bits should represent 50
  if (((char)*pi) != 50) {
    printf ("UNSAFE\n");
    goto ERROR;
  }
  printf ("SAFE\n");

  return (0);
  ERROR:
  return (-1);
}


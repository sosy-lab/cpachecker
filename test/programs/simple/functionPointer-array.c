// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>
int a()
{
  printf("a\n");
}

int b()
{
  printf("b\n");
}  


int main()
{
  int (*p[2])();
  
  p[0] = a;
  p[1] = b;
  p[0]();
  p[1]();
}



// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include "hello.h"

int GLOBAL = 5;
int a;
int a;
int a;
int a;
int a = 2;
int a;
static int x = 0;
struct header_struct A = {.a=5};
static int sameName = 3;


static int mal3(int x) {
  return 3*x;
  return x;
}

int main (void) {
  static int d = 2;

  d = 10;
  d = mal3(d);
  hello ("world");
  return x;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

//#include <stdio.h>
#include "hello.h"

struct header_struct Aa = {.a=5};
static int sameName = 4;


void hello (const char * name) {
  struct header_struct s;
  s.a = 1;
 // printf ("Hello, %s!\n", name);
}

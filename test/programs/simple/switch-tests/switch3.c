// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int main(void) {

int a = 1;
int b = 1;

switch (a) {
  case 1 :
    b = 3;
    break;
  
  case 2 :
    b = 5;
    break;
}

return (0);
}

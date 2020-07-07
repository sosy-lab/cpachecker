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

switch (a) {
  case 1 :
    a = 1;
  break;
  
  case 2 :
    a = 1;

  case 3 :
    a = 1;

  case 4 :
    a = 1;
  
  case 5 :
    a = 1;
    break;
}

return (0);
}

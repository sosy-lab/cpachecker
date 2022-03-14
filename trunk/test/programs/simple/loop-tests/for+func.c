// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int square(x){
  return (x*x);
}

int cube(x){
  return (x*x*x);
}

int main(void) {

  int counter;
  int x = 3;
  
  for (counter = square(x); counter < 5; counter = cube(counter)) {
    int a = 0;
  }

  return (0);
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int main(void) {

  int counter = 0;

  while (counter < 5) {
    counter++;
  
    int counter2 = 10;
    while(counter2 > 0) {
      counter2--;
    }
    
    if (10 == counter) {
      goto ERROR;
    }
  }

  while (counter < 10) {
  counter++;
  }
  
  return (0);

  ERROR:
    goto ERROR;

}

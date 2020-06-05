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
  
  for (; ; ) {
    int a;
    counter++;
    
    if (3 == counter){
      continue;
    }
    
    if (counter > 5){
      break;
    }
  }

  return (0);
}

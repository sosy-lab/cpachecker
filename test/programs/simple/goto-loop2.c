// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int main(void) {
   
  int counter1 = 0;

LOOPSTART1:
  if (counter1 < 5) {
        
    int counter2 = 0;
    
    LOOPSTART2:
      if (counter2 < 8) {
	counter2++;
        goto LOOPSTART2;
    }
    
    counter1++;
    goto LOOPSTART1;
  } 

if (counter1 == 4) {
  goto ERROR;
} else {
  goto END;
}

ERROR: 
  goto ERROR;

END:
  return (0);
}
 

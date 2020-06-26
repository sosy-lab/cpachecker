// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int main(void) {

  int counter;

  for (counter = 0; counter < 5; counter++) {
    
    if (2 == counter) {
      continue;
    }
    
    if (3 == counter) {
      continue;
    }
    
    if (4 == counter) {
      continue;
    }
  }

  return (0);
}

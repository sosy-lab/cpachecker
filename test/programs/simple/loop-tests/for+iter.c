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

  for (counter = 0; counter < 5; counter) {
    int a;
    counter++;
  }
  
  for (counter = 0; counter < 5; ) {
    int b;
    counter++;
  }

  return (0);
}

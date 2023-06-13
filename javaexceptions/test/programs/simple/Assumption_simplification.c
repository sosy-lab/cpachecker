// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/* The test should check the simplification of assumptions
 * There was a bug, when if(! a != 0) was converted to if (a != 0)
 */ 

int main() {
  int a = 0;
  if (! a != 0) {
    ERROR:
    0;
  }
}

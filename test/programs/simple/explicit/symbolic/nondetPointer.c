// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int var = 100;
  int * p1 = &var;
  int * p2 = p1;
  
  if (p1 != p2) {
    
  }

  if (p1 == p2) {
    return 0;
  } else {
ERROR:
    return -1;
  }
}

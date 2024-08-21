// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int i = 0;

  while(i<2) {  
       i++;
  }
  if (i != 2) {
     ERROR: return 1;
  }
  else {
     return 0;
  }
}


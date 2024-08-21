// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main()
{
  int x = 0, y = 0;
  int m;
  int n = 2 * m;

  while (x < n) {
    if (x < m) {
      x ++;
      y ++;
    } else {
      x ++;
      y --;
    }
  }
  if(y != 0) {
    ERROR: goto ERROR;
  }
  return 0;
} 

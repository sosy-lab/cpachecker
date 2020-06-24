// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) 
{
int a  ;
int s  ;
  {
    // change the order of the below assignment to make it work
    a = 0;
    s = 1;
    if (a == 1) {
      goto ERROR1;
ERROR1:;
    } else {
      a = 1;
    }
    if (a == 1) {
      if (s == 1) {
      } else {
      goto ERROR2;
ERROR2:;
      }
    } else {
    }

    if (s == 1) {
    } else {
      goto ERROR3;
ERROR3:;
    }
    return (0);
  }
}


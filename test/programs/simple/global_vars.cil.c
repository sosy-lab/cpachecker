// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int GLOBAL;

int main(void) 
{
  int LOCAL;
  int i;

  {
    GLOBAL = 0;
    LOCAL = 0;
    i = GLOBAL; // i = LOCAL; would make it work
    if (i == 1) {
      if (i == 1) {
      } else {
      }
    } else {

    }
  }
}

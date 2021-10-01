// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void)
{

  int i;
  i = 0;

  {
    int i;
    i = 2;

    int i; // should lead to an error
    i = 3;
  }

  return 0;
}
